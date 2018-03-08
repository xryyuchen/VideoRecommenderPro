package com.yuchen.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import com.yuchen.java.model.Constant;
import com.yuchen.server.model.core.User;
import com.yuchen.server.model.request.LoginUserRequest;
import com.yuchen.server.model.request.RegisterUserRequest;
import com.yuchen.server.model.request.UpdateUserGenresRequest;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author yuchen
 * 对于用户具体处理业务服务的服务类
 */
@Service
public class UserService {

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ObjectMapper objectMapper;

    private MongoCollection<Document> userCollection;

    /**
     * 用于获取User表的连接
     * @return
     */
    private MongoCollection<Document> getUserCollection(){
        if(null == userCollection)
            this.userCollection = mongoClient.getDatabase(Constant.MONGO_DATABASE).getCollection(Constant.MONGO_USER_COLLECTION);
        return this.userCollection;
    }

    /**
     * 将user转换成document
     * @param user
     * @return
     */
    private Document userToDocument(User user){
        Document document = null;
        try {
            document = Document.parse(objectMapper.writeValueAsString(user));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return document;
    }

    private User documentToUser(Document document){
        User user = null;
        try {
            user = objectMapper.readValue(JSON.serialize(document),User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return user;
    }

    /**
     * 用于提供注册用户的服务
     * @param request
     * @return
     */
   public boolean registerUser(RegisterUserRequest request) {
       //判断用户是否有相同的用户名已经注册
       if(getUserCollection().find(new Document("username",request.getUsername())).first() != null)
           return false;
       //创建一个用户
       User user = new User();
       user.setUsername(request.getUsername());
       user.setPassword(request.getPassword());
       user.setFirst(true);

       //插入一个用户
       Document document = userToDocument(user);
       if (null == document)
           return false;
       getUserCollection().insertOne(document);
       return true;
    }

    /**
     * 用于提供用户的登录
     * @param request
     * @return
     */
    public boolean loginUser(LoginUserRequest request) {
        //需要找到这个用户
        Document document = getUserCollection().find(new Document("username",request.getUsername())).first();
        if (null == document)
            return false;
        User user = documentToUser(document);

        if (null == user)
            return false;
        return user.getPassword().compareTo(request.getPassword()) == 0;
    }

    /**
     * 用于更新用户第一次登录选择的电影类别
     * @param request
     */
    public void updateUserGenres(UpdateUserGenresRequest request) {
        getUserCollection().updateOne(new Document("username",request.getUsername()),new Document().append("$set",new Document("$genres",request.getGenres())));
        getUserCollection().updateOne(new Document("username",request.getUsername()),new Document().append("$set",new Document("$first",false)));
    }

    //通过用户名查询一个用户
    public User findUserByUsername(String username){
        Document document = getUserCollection().find(new Document("username",username)).first();
        if(null == document || document.isEmpty())
            return null;
        return documentToUser(document);
    }
}
