package com.example.smartbugdet.network;

import com.example.smartbugdet.model.Transaction;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AllTransactionsResponse {

    @SerializedName("transactions")
    private List<Transaction> transactions;

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
