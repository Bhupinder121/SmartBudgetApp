package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class SpendingLimitRequest {

    @SerializedName("newLimit")
    private double newLimit;

    public SpendingLimitRequest(double newLimit) {
        this.newLimit = newLimit;
    }

    public double getNewLimit() {
        return newLimit;
    }

    public void setNewLimit(double newLimit) {
        this.newLimit = newLimit;
    }
}
