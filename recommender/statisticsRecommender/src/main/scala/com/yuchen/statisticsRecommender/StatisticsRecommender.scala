package com.yuchen.statisticsRecommender

import java.text.SimpleDateFormat
import java.util.Date
import com.yuchen.java.model.Constant._
import com.yuchen.scala.model._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

object StatisticsRecommender {

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://yuchen:27017/recommender",
      "mongo.db" -> "recommender"
    )
    // 创建sparkConf
    val sparkConf = new SparkConf().setAppName("StatisticsRecommender").setMaster(config.get("spark.cores").get)
    // 创建SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    val mongoConfig = MongoConfig(config("mongo.uri"),config("mongo.db"))

    // 导入隐示转换
    import spark.implicits._

    //加载movie数据的RDD
    val ratingDF = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieRating]
      .toDF()

    val movieDF = spark
      .read
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Movie]
      .toDF()

    //创建一张ratings的表
    ratingDF.createOrReplaceTempView("ratings")

    //统计所有历史数据中每个电影的评分次数
    //数据结构--- mid,count
    val rateMoreMoviesDF = spark.sql("select mid,count(mid) as count from ratings group by mid")
    storeDataToMongoDB(rateMoreMoviesDF,MONGO_RATING_COLLECTION,mongoConfig)
    /*rateMoreMoviesDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",RATE_MORE_MOVIES)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()*/

    //统计以月为单位每个电影的评分数
    //数据结构 -- mid，count，time

    //创建一个日期格式化工具
    val simpleDataFormat = new SimpleDateFormat("yyyyMM")

    //注册一个UDF函数，用于将timestamp转换成年月格式 1250759144000 - 201605
    spark.udf.register("changeDate",(x:Int) => simpleDataFormat.format(new Date(x * 1000L)).toInt)

    //将原来的Rating数据集中的时间转换成年月格式
    val ratingOfYearMounth = spark.sql("select mid,score,changeDate(timestamp) as yearmounth from ratings")

    //将新的数据集注册成为一张表
    ratingOfYearMounth.createOrReplaceTempView("ratingOfMounth")

    val rateMoreRecentlyMovies = spark.sql("select mid,count(mid) as count, yearmounth from ratingOfMounth group by yearmounth,mid")
    //保存最近评分电影
    storeDataToMongoDB(rateMoreRecentlyMovies,MONGO_RATE_RECENTLY_MOVIES,mongoConfig)
    /*rateMoreRecentlyMovies
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",RATE_MORE_RECENTLY_MOVIES)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()*/

    //统计每个电影的平均评分
    val averageMoviesDF = spark.sql("select mid,avg(score) as avg from ratings group by mid")

    //保存电影平均得分电影
    storeDataToMongoDB(averageMoviesDF,MONGO_AVERAGE_MOVIES,mongoConfig)
    /*averageMoviesDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",AVERAGE_MOVIES)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()*/

    //统计每种电影类型中评分最高i的10个电影
    //需要使用left join，应为只需要有评分的电影数据集
    val movieWithScore  = movieDF.join(averageMoviesDF,Seq("mid","mid"))

    //所有电影类别
    val genres = List("Action","Adventure","Animation","Comedy","Ccrime","Documentary","Drama","Family","Fantasy","Foreign","History","Horror","Music","Mystery"
      ,"Romance","Science","Tv","Thriller","War","Western")

    //将电影类别转换成RDD
    val genresRDD = spark.sparkContext.makeRDD(genres)

    //通过explode来实现
    import org.apache.spark.sql.functions._
    spark.udf.register("splitGe",(x:String) => x.split("\\|"))
    val singleGenresMovies = movieWithScore.select($"mid",$"avg",explode(callUDF("splitGe",$"genres")).as("genre"))
    val genrenTopMovies = singleGenresMovies
      .rdd
      .map(row => (row.getAs[String]("genre"),(row.getAs[Int]("mid"),row.getAs[Double]("avg"))))
      .groupByKey()
      .map{
        case (genres,items) => GenresRecommendation(genres,items.toList.sortWith(_._2 > _._2).take(10).map(item => Recommendation(item._1,item._2)))
      }.toDF()


  /*
    //计算电影类别的top10
    val genrenTopMovies = genresRDD.cartesian(movieWithScore.rdd)  //将电影类别和电影数据进行笛卡儿积操作
      .filter{
      //过滤掉类别不匹配的电影
      case (genres,row) => row.getAs[String]("genres").toLowerCase.contains(genres.toLowerCase())
      }
      .map{
        //将整个数据集的数据量减少，生成RDD{String,Iter[mid,avg]]
        case (genres,row) => {
          (genres,(row.getAs[Int]("mid"),row.getAs[Double]("avg")))
        }
      }
        .groupByKey()
      .map{
        //通过评分的大小进行数据的排序，然后将数据映射为对象
        case (genres,items) => GenresRecommendation(genres,items.toList.sortWith(_._2 > _._2).take(10).map(item => Recommendation(item._1,item._2)))
      }.toDF()

  */

    //输出数据到monfoDB
    storeDataToMongoDB(genrenTopMovies,MONGO_GENRES_TOP_MOVIES,mongoConfig)
    /*genrenTopMovies
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",GENRES_TOP_MOVIES)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()*/

    spark.close()


  }

  //保存数据到mongodb
  def storeDataToMongoDB(dataDF:DataFrame,collection:String,mongoConfig:MongoConfig): Unit = {
    dataDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",collection)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }

}





































