import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType}
import org.apache.spark.sql.{Column, DataFrame, SparkSession}

import scala.collection.JavaConverters._
import scala.language.postfixOps

object FullyStreamingApp extends App {

  val spark = SparkSession.builder()
    .master("local[*]")
    .appName("spark-app")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  import spark.implicits._

  private val column: Column = split('labelWord, ";")

  val tokenizer = new Tokenizer()
    .setInputCol("words")
    .setOutputCol("tokens")

  val hashingTF = new HashingTF()
    .setNumFeatures(1000)
    .setInputCol(tokenizer.getOutputCol)
    .setOutputCol("features")

  val lr = new LogisticRegression()
    .setMaxIter(15)
    .setRegParam(0.01)

  val pipeline = new Pipeline()
    .setStages(Array(tokenizer, hashingTF, lr))

  var dummyCounter: Int = 0
  while (true) {
    val topic = s"topic-${dummyCounter % 2}"
    println(s"learning from topic: $topic")
    val trainSet = spark.read
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", topic)
      .load.select('value cast StringType as "labelWord")
      .filter(!isnull('labelWord) && 'labelWord.contains(";"))
      .select(column.getItem(0).as("label") cast IntegerType, column.getItem(1).as("words"))

    val model = pipeline.fit(trainSet)
    val words = col("value") cast StringType as "words"
    val streamingDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "input")
      .load.select('value cast StringType as "words")

    val frame: DataFrame = model
      .transform(streamingDF).select("probability", "prediction")
    val query = frame.writeStream
      .format("console")
      .start
    query.awaitTermination(30000)
    println(s"finished to learn from $topic")
    dummyCounter += 1
    if (query.isActive) query.stop()
  }
}
