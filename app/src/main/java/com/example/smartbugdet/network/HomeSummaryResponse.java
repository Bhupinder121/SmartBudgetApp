package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class HomeSummaryResponse {

    @SerializedName("total_balance")
    private double totalBalance;

    @SerializedName("daily_spending_limit")
    private double dailySpendingLimit;

    @SerializedName("income") // Assuming the key from backend for today's income
    private double incomeToday;

    @SerializedName("expense") // Assuming the key from backend for today's expense
    private double expenseToday;

    // Getters
    public double getTotalBalance() {
        return totalBalance;
    }

    public double getDailySpendingLimit() {
        return dailySpendingLimit;
    }

    public double getIncomeToday() {
        return incomeToday;
    }

    public double getExpenseToday() {
        return expenseToday;
    }

    // toString for debugging
    @Override
    public String toString() {
        return "HomeSummaryResponse{" +
               "totalBalance=" + totalBalance +
               ", dailySpendingLimit=" + dailySpendingLimit +
               ", incomeToday=" + incomeToday +
               ", expenseToday=" + expenseToday +
               '}';
    }
}