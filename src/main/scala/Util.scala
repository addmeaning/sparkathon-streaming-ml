import java.io.{File, PrintWriter}

import scala.util.Random

object Util {
  def generateCenters(x: Double, y: Double, z: Double, n: Int, deviation: Int, trueCategory: Int): Seq[(String, String)] = {
    val random = new Random()
    (1 to n).map(n => {
      def vector =
        Vector( x + deviation * random.nextDouble,
                y + deviation * random.nextDouble,
                z + deviation * random.nextDouble() ).mkString("[", ",", "]")
      (vector, s"($trueCategory, $vector)")
    })
  }


  def writeToFile(trainingVectors: Seq[String], testVectors: Seq[String], name: String): Unit = {
    val training = new File("src/main/resources/kmeans/training", name)
    val test = new File("src/main/resources/kmeans/test", name)

    def writeToFile(file: File, vectors: Seq[String]): Unit = {
      if (file.exists()) file.delete()
      file.createNewFile()
      val writer = new PrintWriter(file)
      vectors.foreach(x =>
        writer.write(x + "\n")
      )
      writer.close()
    }

    writeToFile(training, trainingVectors)
    writeToFile(test, testVectors)
  }

  def main(args: Array[String]): Unit = {
    def initDir(path: String) = {
      val folder = new File(s"src/main/resources/kmeans/$path")
      if (!folder.exists()) folder.mkdirs()
      else folder.listFiles().foreach(_.delete())
    }

    initDir("training")
    initDir("test")

    for (i <- 0 to 100) {
      val (training, test) =
        (generateCenters(10.0, 10.0, 10.0, 3, 6, 0) ++
          generateCenters(-10.0, -10.0, 10.0, 3, 6, 1) ++
          generateCenters(-10.0, 10.0, -10.0, 3, 6, 2) ++
          generateCenters(10.0, -10.0, -10.10, 3, 6, 3)).unzip
      writeToFile(training, test, s"${i % 20}.txt")
      println("12 new vectors added, sleeping 5 seconds")
      Thread.sleep(5000) //Don't do this in serious applications
    }

  }
}
