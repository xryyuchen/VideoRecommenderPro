package com.yuchen.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.util.JSON;
import com.yuchen.java.model.Constant;
import com.yuchen.server.model.core.Tag;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TagService {

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ObjectMapper objectMapper;

    private Tag documentToTag(Document document){
        Tag tag = null;
        try {
            tag = objectMapper.readValue(JSON.serialize(document),Tag.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tag;
    }

    private Document tagToDocument(Tag tag){
        Document document = null;
        try {
            document = Document.parse(objectMapper.writeValueAsString(tag));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
     * 获取电影标签
     * @param mid
     * @return
     */
    public List<Tag> getMovieTags(int mid){
        MongoCollection<Document> tagCollection = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_TAG_COLLECTION);
        FindIterable<Document> documents = tagCollection.find(Filters.eq("mid",mid));
        List<Tag> tags = new ArrayList<>();
        for(Document item:documents){
            tags.add(documentToTag(item));
        }
        return tags;
    }

    /**
     * 给电影加标签
     * @param tag
     */
    public void addTagToMovie(Tag tag){
        MongoCollection<Document> tagCollection = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_TAG_COLLECTION);
        tagCollection.insertOne(tagToDocument(tag));
    }


}
