import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.language.postfixOps

object SparkApp extends App {

  val spark = SparkSession.builder()
    .master("local[*]")
    .appName("spark-app")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  import spark.implicits._


  val trainset = spark.createDataFrame(Seq(
    (1L, 1, "spark rocks"),
    (2L, 0, "flink is the best"),
    (3L, 1, "Spark rules"),
    (3L, 1, "cool spark"),
    (4L, 0, "mapreduce forever"),
    (5L, 0, "Kafka is great"),
    (5L, 1, "Apache Spark to rule them all"),
    (5L, 0, "Apache Kafka is distributed"),
    (6L, 0, "Apache flink is ok")
  )).toDF("id", "label", "words")

  private val column = split('labelWord, ";")


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
  val model = pipeline.fit(trainset)

  val streamingDF = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "hello")
    .load.select('value cast StringType as "words")

  private val frame: DataFrame = model
    .transform(streamingDF).select("probability", "prediction")
  val query = frame.writeStream
    .format("console")
    .start
  query.awaitTermination()
}
