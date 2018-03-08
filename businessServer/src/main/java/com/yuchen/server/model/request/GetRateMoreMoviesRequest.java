package com.yuchen.server.model.request;

/**
 * @author yuchen
 * 获取优质电影
 */
public class GetRateMoreMoviesRequest {
    private int num;

    public GetRateMoreMoviesRequest(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
