package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("expire_at") // Or "expires_at", "expiration", etc., match your API
    private String expireAt;

    @SerializedName("message")
    private String message;

    // Getters
    public String getToken() {
        return token;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public String getMessage() {
        return message;
    }

    // Setters can be added if needed, but often aren't for response objects
    // public void setToken(String token) { this.token = token; }
    // public void setExpireAt(String expireAt) { this.expireAt = expireAt; }
    // public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='" + (token != null ? "PRESENT" : "null") + '\'' +
                ", expireAt='" + expireAt + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}