package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class UserMetadataRequest {

    @SerializedName("total_balance")
    private double startingBalance;

    @SerializedName("daily_spending_limit")
    private double dailySpendingLimit;

    public UserMetadataRequest(double startingBalance, double dailySpendingLimit) {
        this.startingBalance = startingBalance;
        this.dailySpendingLimit = dailySpendingLimit;
    }

    // Getters (and setters if needed, though typically not for request objects)
    public double getStartingBalance() {
        return startingBalance;
    }

    public double getDailySpendingLimit() {
        return dailySpendingLimit;
    }
}