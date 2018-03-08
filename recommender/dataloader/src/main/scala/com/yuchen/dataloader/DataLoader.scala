package com.yuchen.dataloader

import java.net.InetAddress

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

import com.yuchen.java.model.Constant._

import com.yuchen.scala.model._



//数据的主加载服务
object DataLoader {
  val MOVIE_DATA_PATH = "H:\\Intellij_wokspace\\VideoRecommenderPro\\recommender\\dataloader\\src\\main\\resources\\movies.csv"
  val RATINNG_DATA_PATH = "H:\\Intellij_wokspace\\VideoRecommenderPro\\recommender\\dataloader\\src\\main\\resources\\ratings.csv"
  val TAG_DATA_PATH = "H:\\Intellij_wokspace\\VideoRecommenderPro\\recommender\\dataloader\\src\\main\\resources\\tags.csv"

  // 程序的入口
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://yuchen:27017/recommender",
      "mongo.db" -> "recommender",
      "es.httpHosts" -> "yuchen:9200",
      "es.transportHosts" -> "yuchen:9300",
      "es.index" -> "recommender",
      "es.cluster.name" -> "es-cluster"
    )

    // 创建sparkConf
    val sparkConf = new SparkConf().setAppName("DataLoser").setMaster(config.get("spark.cores").get)

    // 创建SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    // 导入隐示转换
    import spark.implicits._

    // 将Movie，Rating，Tag数据集加载进来
    val movieRDD = spark.sparkContext.textFile(MOVIE_DATA_PATH)
    val movieDF = movieRDD.map(item => {
      val attr = item.split("\\^")
      Movie(attr(0).toInt,attr(1).trim,attr(2).trim,attr(3).trim,attr(4).trim,attr(5).trim,attr(6).trim,attr(7).trim,attr(8).trim,attr(9).trim)
    }).toDF()

    val ratingRDD = spark.sparkContext.textFile(RATINNG_DATA_PATH)
    val ratingDF = ratingRDD.map(item => {
      val attr = item.split(",")
      MovieRating(attr(0).toInt,attr(1).toInt,attr(2).toDouble,attr(3).toInt)
    }).toDF()

    val tagRDD = spark.sparkContext.textFile(TAG_DATA_PATH)
    val tagDF = tagRDD.map(item => {
      val attr = item.split(",")
      Tag(attr(0).toInt,attr(1).toInt,attr(2).trim,attr(3).toInt)
    }).toDF()

    implicit  val mongoCinfig = MongoConfig(config.get("mongo.uri").get,config.get("mongo.db").get)

    // 需要将数据保存到MongoDB中
    storeDataInMongoDB(movieDF,ratingDF,tagDF)


//    // 首先需要将Tag数据集进行处理，处理后的形式为 MID，tag1|tag2|tag3 tag1 tag2 tag3
//    import org.apache.spark.sql.functions._
//
//    /**
//      * MID , Tags
//      * 1     tag1|rag2|tag3......
//      */
//    val newTag = tagDF.groupBy($"mid").agg(concat_ws("|",collect_set($"tag")).as("tags")).select("mid","tags")
//
//    // 需要将处理后的Tag数据，和Movie数据融合，产生新的Movie数据
//    val movieWithTagsDF = movieDF.join(newTag,Seq("mid","mid"),"left")
//
//    // 声明一个ES配置的隐式参数
//    implicit val esConfig = ESConfig(config.get("es.httpHosts").get,config.get("es.transportHosts").get,config.get("es.index").get,config.get("es.cluster.name").get)
//
//    // 需要将新的Movie数据保存到ES中
//    storeDataInES(movieWithTagsDF)

    // 关闭Spark
    spark.stop()
  }

  // 间数据保存到MongoDB中的方法
  def storeDataInMongoDB(movieDF:DataFrame,ratingDF:DataFrame,tagDF:DataFrame)(implicit  mongoConfig:MongoConfig): Unit ={
    //新建一个到MongoDB的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    //如果MongoDB中有对应的数据库，那么应该删除
    mongoClient(MONGO_DATABASE)(MONGO_MOVIE_COLLECTION).dropCollection()
    mongoClient(MONGO_DATABASE)(MONGO_RATING_COLLECTION).dropCollection()
    mongoClient(MONGO_DATABASE)(MONGO_TAG_COLLECTION).dropCollection()

    //将当前数据写入到MongoDB
    movieDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_MOVIE_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    ratingDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    tagDF
      .write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGO_TAG_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //对数据表捡索引
    mongoClient(MONGO_DATABASE)(MONGO_MOVIE_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(MONGO_DATABASE)(MONGO_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(MONGO_DATABASE)(MONGO_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(MONGO_DATABASE)(MONGO_TAG_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(MONGO_DATABASE)(MONGO_TAG_COLLECTION).createIndex(MongoDBObject("uid" -> 1))

    // 关闭MongoDB的连接
    mongoClient.close()
  }

  // 将数据保存到ES中的方法
  def storeDataInES(movieDF: DataFrame)(implicit esConfig:ESConfig): Unit ={

    //新建一个配置
    val settings:Settings = Settings.builder().put("cluster.name",esConfig.clustername).build()

    //新建一个ES的客户端
    val esClient = new PreBuiltTransportClient(settings)

    //需要将TransportHosts添加到esClient中
    val REGEX_HOST_PORT = "(.+):(\\d+)".r
    esConfig.transportHosts.split(",").foreach {
      case REGEX_HOST_PORT(host: String, port: String) => {
        esClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port.toInt))
      }
    }

    //需要清除掉ES中的遗留数据
    if(esClient.admin().indices().exists(new IndicesExistsRequest(esConfig.index)).actionGet().isExists){
      esClient.admin().indices().delete(new DeleteIndexRequest(esConfig.index))
    }
    esClient.admin().indices().create(new CreateIndexRequest(esConfig.index))

    //将数据写入到ES中
    movieDF
      .write
      .option("es.nodes",esConfig.httpHosts)
      .option("es.http.timeout","100m")
      .option("es.mapping.id","mid")
      .mode("overwrite")
      .format("ES_DRIVER_CLASS")
      .save(esConfig.index+"/"+ES_TYPE)

  }

}





