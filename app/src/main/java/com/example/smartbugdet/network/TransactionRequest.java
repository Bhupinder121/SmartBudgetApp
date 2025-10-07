package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName; // Added for @SerializedName

public class TransactionRequest {
    private String title;
    private double amount;
    private String type; // "income" or "expense"
    private String date; // Format: "YYYY-MM-DD"

    @SerializedName("pre_amount") // This will be the key in the JSON sent to the server
    private double balanceBeforeTransaction; // Field name in client code

    public TransactionRequest(String title, double amount, String type, String date, double balanceBeforeTransaction) {
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.date = date;
        this.balanceBeforeTransaction = balanceBeforeTransaction;
    }

    // Getters and Setters can be added if needed.
    // For example:
    // public double getBalanceBeforeTransaction() {
    //     return balanceBeforeTransaction;
    // }
}
