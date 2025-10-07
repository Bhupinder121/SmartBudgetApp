package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class UserSetupStatusResponse {

    @SerializedName("setup_complete")
    private boolean setupComplete;

    @SerializedName("message")
    private String message; // Optional: May or may not be present in all responses

    // Getters
    public boolean isSetupComplete() {
        return setupComplete;
    }

    public String getMessage() {
        return message;
    }

    // It's good practice to have a toString() for debugging
    @Override
    public String toString() {
        return "UserSetupStatusResponse{" +
               "setupComplete=" + setupComplete +
               ", message='" + message + '\'' +
               '}';
    }
}