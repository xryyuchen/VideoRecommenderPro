package com.yuchen.server.model.request;

/**
 * @author yuchen
 * 获取最新电影
 */
public class GetNewMoviesRequest {
    private int num;

    public GetNewMoviesRequest(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
