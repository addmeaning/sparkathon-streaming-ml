import org.apache.spark.SparkConf
import org.apache.spark.mllib.clustering.StreamingKMeans
import org.apache.spark.mllib.linalg.{Vectors, Vector => SparkVector}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}


object StreamingKmeans extends App {

  val conf = new SparkConf().setAppName("StreamingKMeansExample").setMaster("local[*]")
  val ssc = new StreamingContext(conf, Seconds(5))
  ssc.sparkContext.setLogLevel("WARN")

  val trainingData: DStream[SparkVector] = ssc.textFileStream("src/main/resources/kmeans/training").map(Vectors.parse)
  val testData = ssc.textFileStream("src/main/resources/kmeans/test").map(LabeledPoint.parse)

  val model = new StreamingKMeans()
    .setK(4)
    .setDecayFactor(0)
    .setRandomCenters(3, 0.5)

  model.trainOn(trainingData)
  model.predictOnValues(testData.map(lp => (lp.label, lp.features))).print(12)

  ssc.start()
  ssc.awaitTermination()
}

