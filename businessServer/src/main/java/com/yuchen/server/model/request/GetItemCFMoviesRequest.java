package com.yuchen.server.model.request;

/**
 * @author yuchen
 * 获取嗲应相似矩阵中的结果
 */
public class GetItemCFMoviesRequest {
    private int mid;
    private int num;

    public GetItemCFMoviesRequest(int mid, int num) {
        this.mid = mid;
        this.num = num;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
