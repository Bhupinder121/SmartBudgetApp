package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class GenericMessageResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}