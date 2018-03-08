package com.tyuchen.offlineRecommender

import com.yuchen.java.model.Constant._
import com.yuchen.scala.model._
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

object OfflineRecommender {

  //入口方法
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://yuchen:27017/recommender",
      "mongo.db" -> "recommender"
    )
    //创建与i个SparkConf配置
    val sparkConf = new SparkConf().setAppName("OfflineRecommender").setMaster(config("spark.cores"))
      .set("spark.excutor.memory","4G").set("spark.driver.memory","1G")
    //创建SparKSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    //创建一个MonogoDBConfig
    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    import spark.implicits._

    //读取mongoDB中的数据
    val ratingRDD = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieRating]
      .rdd
      .map((rating => (rating.uid,rating.mid,rating.score))).cache()

    //用户的数据集 RDD[Int,Int]
    val userRDD = ratingRDD.map(_._1).distinct().cache()

    //电影的数据集RDD{Int]
    val movieRDD = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Movie]
      .rdd
      .map(_.mid).cache()

    //创建训练数据
    val trainData = ratingRDD.map(x => Rating(x._1,x._2,x._3)).cache()

    val (rank,iterations,lambda) = (50,10,0.01)
    //训练ALS模型
    val model = ALS.train(trainData,rank,iterations,lambda)



//    //计算用户推荐矩阵
//    //构建一个usersProducts RDD[(INT,Int)]
//    val userMovies = userRDD.cartesian(movieRDD)
//    val preRatings = model.predict(userMovies)
//    val userRecs = preRatings
//        .filter(_.rating > 0)
//        .map(rating => (rating.user,(rating.product,rating.rating)))
//        .groupByKey()
//        .map{
//          case (uid,recs) => UserRecs(uid,recs.toList.sortWith(_._2 > _._2).take(USER_MAX_RECOMMENDATION).map(x => Recommendation(x._1,x._2)))
//        }.toDF()
//
//    userRecs
//      .write
//      .option("uri",mongoConfig.uri)
//      .option("collection",MONGO_USER_RECS_COLLECTION)
//      .mode("overwrite")
//      .format("com.mongodb.spark.sql")
//      .save()
//

    //计算电影详细矩阵
    var movieFeatures = model.productFeatures.map {
      case (mid, freatures) => (mid, new DoubleMatrix(freatures))
    }
    val movieRecs = movieFeatures.cartesian(movieFeatures)
      .filter{
        case (a,b) => a._1 != b._1
      }
      .map{
        case (a,b) =>
          val simScore = this.consinSim(a._2,b._2)
          (a._1,(b._1,simScore))
      }.filter(_._2._2 > 0.6)
      .groupByKey()
      .map{
        case (mid,items) =>
          MovieRecs(mid,items.toList.map(x => Recommendation(x._1,x._2)))
      }.toDF().cache()
    movieRecs
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_MOVIE_RECS_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //关闭Spark
    spark.close()

  }

  def consinSim(m1:DoubleMatrix,m2:DoubleMatrix) : Double = {
    m1.dot(m2) / (m1.norm2() * m2.norm2())
  }


}

















