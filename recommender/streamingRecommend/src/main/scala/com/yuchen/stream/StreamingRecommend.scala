package com.yuchen.stream

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.yuchen.stream.ConnHelper.mongoClient
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.JavaConversions._

case class MongoConfig(uri:String,db:String)
//推荐
case class Recommendation(rid:Int,r:Double)

//用户推荐
case class UserRecs(uid:Int,recs:Seq[Recommendation])
//电影推荐
case class MovieRecs(mid:Int,recs:Seq[Recommendation])

object StreamingRecommend {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_MOVIES_NUM = 20
  val MONGODB_STREAN_RECS_COLLECTION = "streamRecs"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_MOVIE_RECS_COLLECTION = "moviesRecs"
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://yuchen:27017/recommender",
      "mongo.db" -> "recommender",
      "kafka.topic" -> "recommender"
    )
    //创建SparkConfig
    val sparkConf = new SparkConf().setAppName("StreamingRecommend").setMaster(config("spark.cores"))
//      .set("spark.excutor.memory","3G").set("spark.driver.memory","2G")
    //创建SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc,Seconds(2))

    implicit val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))
    import spark.implicits._


//    /************** 广播电影相似度矩阵 **************/
//    val simMoviesMartrix = spark
//      .read
//      .option("uri",mongoConfig.uri)
//      .option("collection",MONGODB_MOVIE_RECS_COLLECTION)
//      .format("com.mongodb.spark.sql")
//      .load()
//      .as[MovieRecs]
//      .rdd
//      .map{recs =>
//        (recs.mid,recs.recs.map(x => (x.rid,x.r)).toMap)
//      }.collectAsMap()
//
//    val simMoviesMartrixBroadCast = sc.broadcast(simMoviesMartrix)
//
//    /*********测试广播***********/
//    val  abc = sc.makeRDD(1 to 2)
//    abc.map(x => simMoviesMartrixBroadCast.value.get(1)).count()
//    /**********广播结束**************************/


    val kafkaParas = Map(
      "bootstrap.servers" -> "yuchen:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "recommender",
      "auto.offset.reset" ->"latest"
    )
    val kafkaStreaming = KafkaUtils.createDirectStream[String,String](ssc,LocationStrategies.PreferConsistent,ConsumerStrategies.Subscribe[String,String](Array(config("kafka.topic")),kafkaParas))

    //UID|MID|SCORE|TIMESTAMP
    //产生评分流
    val ratingStream = kafkaStreaming.map{
      case msg =>
        var attr = msg.value().split("\\|")
        (attr(0).toInt,attr(1).toInt,attr(2).toDouble,attr(3).toInt)
    }

//    val recentRatings = scala.collection.mutable.ArrayBuffer[(Int,Int,Double)]()


    ratingStream.foreachRDD{
      rdd =>
        rdd.map{
          case (uid,mid,score,timestamp) =>
            println(">>>"+uid+"\t"+mid+"\t"+score+"\t"+timestamp)

            //获取当前最近的M次电影
            val userRecentlyRatings = getUserRecentlyRating(MAX_USER_RATINGS_NUM,uid,ConnHelper.jedis)

            //
            val allSimMovies = getAllSimMovies _

            //获取电影p最相似的k个电影（除去用户已看过的）
            val simMovies = getTopSimMovies(allSimMovies,MAX_SIM_MOVIES_NUM,mid,uid)

            //计算待选电影的推荐优先级
            val streamRecs = computeMovieScores(allSimMovies,userRecentlyRatings,simMovies)

            //将数据保存到MongoDB
            saveRecsToMongoDB(uid,streamRecs)
            println(">>> ok")
        }.count()
    }
    //启动Streaming程序
    ssc.start()
    ssc.awaitTermination()

  }

  /**
    * 将数据保存到MongoDB uid -> 1, recs -> 22:4.5|45:2.7
    * @param uid                用户ID
    * @param streamRecs         测试的推荐结果
    * @param mongoConfig        MongoDFB的配置
    */
  def saveRecsToMongoDB(uid: Int, streamRecs: Array[(Int,Double)])(implicit mongoConfig: MongoConfig): Unit ={
    //连接StreamRecs
    val streamingRecsCollection = mongoClient(mongoConfig.db)(MONGODB_STREAN_RECS_COLLECTION)
    streamingRecsCollection.findAndRemove(MongoDBObject("uid" -> uid))
    streamingRecsCollection.insert(MongoDBObject("uid" -> uid,"recs" -> streamRecs.map(x => x._1+":"+x._2).mkString("|")))
  }

  /**
    * 计算待选电影的推荐分数
    * @param simMovies              电影相似度矩阵
    * @param userRecentlyRatings    用户最近的K次评分
    * @param topSimMovies           当前电影最相似的k个电影
    * @return
    */
  def computeMovieScores(getAllSimMovies: (Int) => Array[(Int, Double)], userRecentlyRatings: Array[(Int,Double)], topSimMovies: Array[Int]) : Array[(Int,Double)] = {

    //用于保存每一个待选电影和最近评分的每一个电影的权重的分
    val score = scala.collection.mutable.ArrayBuffer[(Int,Double)]()

    //用于保存每一电影分增强因子
    val increMap = scala.collection.mutable.HashMap[Int,Int]()

    //用于保存每一个电影的减弱因子
    val decreMap = scala.collection.mutable.HashMap[Int,Int]()

    for (topSimMovie <- topSimMovies;userRecentlyRating <- userRecentlyRatings){
      val simScore = getMoviesSimScore(getAllSimMovies,userRecentlyRating._1,topSimMovie)
      if(simScore > 0.6){
        score += ((topSimMovie,simScore * userRecentlyRating._2))
        if(userRecentlyRating._2 > 3){
          increMap(topSimMovie) = increMap.getOrDefault(topSimMovie,0) + 1
        }else{
          decreMap(topSimMovie) = decreMap.getOrDefault(topSimMovie,0) + 1
        }
      }
    }

    score.groupBy(_._1).map{case (mid,sims) =>
      (mid,sims.map(_._2).sum / sims.length + log(increMap(mid)) - log(decreMap(mid)))
    }.toArray

  }

  //取2的对数
  def log(m:Int) : Double = {
    math.log(m) / math.log(10)
  }

  /**
    * 获取当前电影的相似度矩阵
    * @param mid            当前电影ID
    * @param mongoConfig    mongoDB的配置
    * @return
    */
  def getAllSimMovies(mid: Int)(implicit mongoConfig: MongoConfig) : Array[(Int, Double)] = {
    mongoClient(mongoConfig.db)(MONGODB_MOVIE_RECS_COLLECTION).find(MongoDBObject("mid" -> mid)).toArray.map{ item =>
      item.get("recs").toString.split("}").filter(_.length > 10 ).map{x1 =>
        val attr = x1.split(":")
        (attr(1).split(",")(0).trim.toInt,attr(2).toDouble)
      }
    }.flatMap(it => it.map(x => (x._1,x._2)))
  }
  /**
    * 获取相似电影与当前电影之间的相似度
    * @param simMovies            电影相似度矩阵
    * @param userRatingMovies     用户已经评分的电影
    * @param topSimMovie          候选电影
    * @return
    */
  def getMoviesSimScore(getAllSimMovies: (Int) => Array[(Int, Double)], userRatingMovie: Int, topSimMovie: Int) : Double = {

    val allSimMovies = getAllSimMovies(topSimMovie).toMap
    val moviesRecs = Option(allSimMovies)
    moviesRecs match {
      case Some(sim) => sim.get(userRatingMovie) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  /**
    * 获取当前电影K个相似的电影
    * @param num            相似的电影的数量
    * @param mid            当前电影的ID
    * @param uid            当前电影的评分用户
    * @param simMovies      电影相似度矩阵的广播变量值
    * @param mongoConfig     MongoDB的配置
    * @return
    */
  def getTopSimMovies(getAllSimMovies: (Int) => Array[(Int, Double)],num: Int, mid: Int, uid: Int)(implicit mongoConfig: MongoConfig) : Array[Int] = {
    //从mongoDB中获取与当前电影相似的所有电影
    val allSimMovies = getAllSimMovies(mid)

    //获取用户已经观看过的电影
    val ratingExist = mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).find(MongoDBObject("uid" -> uid)).toArray.map { item =>
      item.get("mid").toString.toInt
    }
    //过滤掉已经评分过的电影，并排序输出
    allSimMovies.filter(x => !ratingExist.contains(x._1)).sortWith(_._2 > _._2).take(num).map(x => x._1)
  }

  /**
    * 获取当前最近的M次电影评分
    * @param num      评分的个数
    * @param uid      评分的用户
    * @param jedis    redis中评分矩阵
    * @return
    */
  def getUserRecentlyRating(num:Int,uid:Int,jedis:Jedis) : Array[(Int,Double)] = {
    //从用户的队列中取出nun个评论
    jedis.lrange("uid:"+uid.toString,0,num).map{item =>
      val attr = item.split(",")
      (attr(0).trim.toInt,attr(1).trim.toDouble)
    }.toArray

  }

}

object ConnHelper extends Serializable{
  lazy val jedis = new Jedis("yuchen")
  lazy val mongoClient = MongoClient(MongoClientURI("mongodb://yuchen:27017/recommender"))
}

























