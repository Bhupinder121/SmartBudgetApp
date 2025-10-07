package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpLoginRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("otp")
    private String otp;

    public VerifyOtpLoginRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }
}