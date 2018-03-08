package com.yuchen.server.model.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author yuchen
 * 电影信息的包装
 */
public class Movie {

    @JsonIgnore
    private int _id;

    private int mid;
    private String descri;
    private String timelong;
    private String issue;
    private String shoot;
    private String language;
    private String genres;
    private String actors;
    private double score;

    public Movie(int mid, String descri, String timelong, String issue, String shoot, String language, String genres, String actors, double score) {
        this.mid = mid;
        this.descri = descri;
        this.timelong = timelong;
        this.issue = issue;
        this.shoot = shoot;
        this.language = language;
        this.genres = genres;
        this.actors = actors;
        this.score = score;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public String getTimelong() {
        return timelong;
    }

    public void setTimelong(String timelong) {
        this.timelong = timelong;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getShoot() {
        return shoot;
    }

    public void setShoot(String shoot) {
        this.shoot = shoot;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getActors() {
        return actors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
