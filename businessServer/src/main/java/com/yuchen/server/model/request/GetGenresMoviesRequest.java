package com.yuchen.server.model.request;

/**
 * @author yuchen
 * 获取电影类别电影数量
 */
public class GetGenresMoviesRequest {
    private String genres;
    private int num;

    public GetGenresMoviesRequest(String genres, int num) {
        this.genres = genres;
        this.num = num;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
