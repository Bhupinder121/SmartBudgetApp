package com.example.smartbugdet.model;

import com.google.gson.annotations.SerializedName;

public class Transaction {

    @SerializedName("transaction_id")
    private String transactionId;

    @SerializedName("title")
    private String title;

    @SerializedName("amount")
    private double amount;

    @SerializedName("type")
    private String type; // "expense" or "income"

    @SerializedName("date")
    private String date; // Format: "YYYY-MM-DD'T'HH:mm:ss.SSS'Z'" (based on previous findings)

    @SerializedName("pre_amount") // Field for balance before this transaction
    private double preAmount;

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getDate() { return date; }
    public double getPreAmount() { return preAmount; } // Getter for preAmount

    // Setters can be added if needed, or a public constructor
}
