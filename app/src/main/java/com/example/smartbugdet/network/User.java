package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;

    // Getters
    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    // toString for debugging
    @Override
    public String toString() {
        return "User{" +
               "fullName='" + fullName + '\'' +
               ", email='" + email + '\'' +
               '}';
    }
}
