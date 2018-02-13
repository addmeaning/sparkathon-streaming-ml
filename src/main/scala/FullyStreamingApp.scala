import java.util.concurrent.ScheduledThreadPoolExecutor

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters._
import util.control.Breaks._
import scala.language.postfixOps

object FullyStreamingApp extends App {

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
    (5L, 0, "Kafka is great")
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

  //  private val streamingTrainset = spark.readStream
  //    .format("kafka")
  //    .option("kafka.bootstrap.servers", "localhost:9092")
  //    .option("subscribe", "training")
  //    .load.select('value cast StringType as "labelWord")
  //    .select(column.getItem(0).as("label") cast IntegerType, column.getItem(1).as("words"))
  //  val streamingQuery = streamingTrainset.writeStream.trigger(Trigger.ProcessingTime(10 seconds))
  //    .format("console").queryName("someName").start()
  //
  //
  //streamingQuery.awaitTermination()
  //  val streamingModel = pipeline.fit(streamingTrainset)


  //  val streamingDF = spark.readStream
  //    .format("kafka")
  //    .option("kafka.bootstrap.servers", "localhost:9092")
  //    .option("subscribe", "input")
  //    .load.select('value cast StringType as "words")
  //
  //  private val frame: DataFrame = model
  //    .transform(streamingDF).select("probability", "prediction")
  //  val query = frame.writeStream
  //    .format("console")
  //    .start
  //  query.awaitTermination()


  val kafkaConsumer = KafkaConsumers.configureConsumer("retrain-0")
  breakable {
    while (true) {
      println("press any key to continue...")
      scala.io.StdIn.readLine()
      val topic = kafkaConsumer.poll(10000).asScala.lastOption.map(_.value()) match {
        case Some(x) => x
        case None =>
          println("no topic selected")
          break
      }

      println(s"learning-from-$topic")

      val trainset = spark.read
        .format("kafka")
        .option("kafka.bootstrap.servers", "localhost:9092")
        .option("subscribe", topic)
        .load.select('value cast StringType as "labelWord")
        .filter(!isnull('labelWord) && 'labelWord.contains(";"))
        .select(column.getItem(0).as("label") cast IntegerType, column.getItem(1).as("words"))
      val model = pipeline.fit(trainset)
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
    }
  }

}

object KafkaConsumers {

  def configureConsumer(topic: String): KafkaConsumer[String, String] = {
    import java.util.Properties

    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")

    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("group.id", "something")
    val consumer = new KafkaConsumer[String, String](props)

    consumer.subscribe(List(topic).asJava)
    consumer
  }
}
