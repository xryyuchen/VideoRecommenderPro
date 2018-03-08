package com.yuchen.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.util.JSON;
import com.yuchen.server.model.core.Movie;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.yuchen.java.model.Constant;


@Service
public class MovieService {
    @Autowired
    private MongoClient mongoClient;
    @Autowired
    private ObjectMapper objectMapper;

    private MongoCollection<Document> movieCollection;

    /**
     * 获取电影表的信息
     *
     * @return
     */
    private MongoCollection<Document> getMovieCollection() {
        if (null == movieCollection)
            this.movieCollection = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_MOVIE_COLLECTION);
        return this.movieCollection;
    }

    /**
     * 电影movie转换成文档
     *
     * @param movie
     * @return
     */
    private Document movieToDocument(Movie movie) {
        Document document = null;
        try {
            document = Document.parse(objectMapper.writeValueAsString(movie));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
     * 文档转换成movie
     * @param document
     * @return
     */
    private Movie documentToMovie(Document document) {
        Movie movie = null;
        try {
            movie = objectMapper.readValue(JSON.serialize(document), Movie.class);
            Document score = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_AVERAGE_MOVIES).find((Filters.eq("mid", movie.getMid()))).first();
            if (score == null || score.isEmpty())
                movie.setScore(0D);
            else
                movie.setScore(score.getDouble("avg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return movie;
    }

    /**
     * 通过list<mid>获取电影信息
     * @param ids
     * @return
     */
    public List<Movie> getMoviesByMids(List<Integer> ids){
        List<Movie> result = new ArrayList<>();
        FindIterable<Document> documents = getMovieCollection().find(Filters.in("mid",ids));
        for(Document item:documents){
            result.add(documentToMovie(item));
        }
        return result;
    }

    /**
     * 获取电影信息
     * @param mid
     * @return
     */
    public Movie findMovieInfo(int mid){
        Document movieDocument =  getMovieCollection().find(new Document("mid",mid)).first();
        if(movieDocument == null || movieDocument.isEmpty())
            return null;
        return documentToMovie(movieDocument);
    }

}
