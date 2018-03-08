import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.yuchen.stream.ConnHelper.mongoClient
import com.yuchen.stream.StreamingRecommend.MONGODB_MOVIE_RECS_COLLECTION

case class MongoConfig(uri:String,db:String)

object MongoTest {
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://yuchen:27017/recommender",
      "mongo.db" -> "recommender"
    )
    implicit val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))
    val mongoClient = MongoClient(MongoClientURI("mongodb://yuchen:27017"))

    mongoClient(mongoConfig.db)("streamRecs").find(MongoDBObject("uid" -> 1)).foreach(println)

//    val allSimMovies = getAllSimMovies(100450)
//    getMoviecs(getAllSimMovies _)


  }

  def getMoviecs( f: (Int) => Array[(Int, Double)]): Unit ={
    println(f(349).toMap.get(1287))

//    val res = f(100450).toMap
  }

  def getAllSimMovies(mid: Int)(implicit mongoConfig: MongoConfig) : Array[(Int, Double)] = {
    mongoClient(mongoConfig.db)(MONGODB_MOVIE_RECS_COLLECTION).find(MongoDBObject("mid" -> mid)).toArray.map{ item =>
      item.get("recs").toString.split("}").filter(_.length > 10 ).map{x1 =>
        val attr = x1.split(":")
        (attr(1).split(",")(0).trim.toInt,attr(2).toDouble)
      }
    }.flatMap(it => it.map(x => (x._1,x._2)))
  }

}
