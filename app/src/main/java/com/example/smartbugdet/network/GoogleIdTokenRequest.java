package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class GoogleIdTokenRequest {

    @SerializedName("token") // Or whatever your backend expects the field to be named
    private String idToken;

    public GoogleIdTokenRequest(String idToken) {
        this.idToken = idToken;
    }

    // Getter (optional, but good practice)
    public String getIdToken() {
        return idToken;
    }
}