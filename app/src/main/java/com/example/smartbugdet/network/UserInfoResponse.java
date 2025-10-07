package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class UserInfoResponse {

    @SerializedName("user")
    private User user;

    // Getter
    public User getUser() {
        return user;
    }

    // toString for debugging
    @Override
    public String toString() {
        return "UserInfoResponse{" +
               "user=" + (user != null ? user.toString() : "null") +
               '}';
    }
}
