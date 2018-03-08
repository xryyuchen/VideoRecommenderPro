package com.yuchen.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.yuchen.java.model.Constant;
import com.yuchen.server.model.core.Rating;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;

@Service
public class RatingService {

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Jedis jedis;

    private Document ratingToDocument(Rating rating){
        Document document = null;
        try {
            document = Document.parse(objectMapper.writeValueAsString(rating));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return document;
    }

    private Rating documentToRating(Document document){
        Rating rating = null;
        try {
            rating = objectMapper.readValue(JSON.serialize(document),Rating.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rating;
    }

    /**
     * 对电影进行评分
     * @param rating
     */
    public void rateToMovie(Rating rating){
        MongoCollection<Document> ratingCollection = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_RATING_COLLECTION);
        ratingCollection.insertOne(ratingToDocument(rating));
        updateRedis(rating);
    }

    /**
     * 跟新redis评分数据
     * @param rating
     */
    private void updateRedis(Rating rating) {
        if(jedis.llen("uid:"+rating.getUid()) >= Constant.USER_RATING_QUEUE_SIZE){
            jedis.rpop("uid:"+rating.getUid());
        }
        jedis.lpush("uid:"+rating.getUid(),rating.getMid()+":"+rating.getScore());
    }

}
