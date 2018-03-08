package com.yuchen.server.model.request;

/**
 * @author yuchen
 * 获取基于内容的推荐请求
 */
public class GetContentBasedRecommendationRequest {
    private int mid;
    private int sum;

    public GetContentBasedRecommendationRequest(int mid, int sum) {
        this.mid = mid;
        this.sum = sum;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
