package com.yuchen.java.model;

/**
 * 定义整个业务系统的常量
 * @author yuchen
 */
public class Constant {

    /**
     * 数据库名
     */
    public static final String MONGO_DATABASE = "recommender";

    /**
     * 电影表名
     */
    public static final String MONGO_MOVIE_COLLECTION = "Movie";

    /**
     * 电影评分的表名
     */
    public static final String MONGO_RATING_COLLECTION = "Rating";

    /**
     * 电影标签的表
     */
    public static final String MONGO_TAG_COLLECTION = "Tag";

    /**
     * 用户表
     */
    public static final String MONGO_USER_COLLECTION = "User";

    /**
     * 电影的平均评分表
     */
    public static final String MONGO_AVERAGE_MOVIES = "AverageMovies";

    /**
     * 电影类别Top10表
     */
    public static final String MONGO_GENRES_TOP_MOVIES = "GenresTopMovies";

    /**
     * 优质电影表
     */
    public static final String MONGO_RATE_MORE_MOVIES = "RateMoreMovies";

    /**
     * 最热电影表
     */
    public static final String MONGO_RATE_RECENTLY_MOVIES = "RateMoreRecentlyMovies";

    /**
     * 用户的推荐矩阵
     */
    public static final String MONGO_USER_RECS_COLLECTION = "UserRecs";

    /**
     * 电影的相似度矩阵
     */
    public static final String MONGO_MOVIE_RECS_COLLECTION = "MovieRecs";

    /**
     * 实时推荐电影表
     */
    public static final String MONGO_STREAM_RECS_COLLECTION = "StreamRecs";

    /**
     * es使用的index
     */
    public static final String ES_INDEX = "recommender";

    /**
     * es使用的Type
     */
    public static final String ES_TYPE = "Movies";

    public static final int USER_RATING_QUEUE_SIZE = 20;

    public static final String USER_RATING_LOG_PREFIX = "USER_RATING_LOG_PREFIX:";
    public static final String ES_DRIVER_CLASS = "org.elasticsearch.spark.sql";

}
