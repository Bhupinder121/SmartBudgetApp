package com.example.smartbugdet.network;

import com.example.smartbugdet.model.Transaction;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TodaysTransactionsResponse {

    @SerializedName("transactions")
    private List<Transaction> transactions;

    public List<Transaction> getTransactions() {
        return transactions;
    }

    // Setter might not be strictly necessary for Gson deserialization
    // but can be useful for testing or manual object creation.
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}