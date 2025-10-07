package com.example.smartbugdet.network;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpSignUpRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("otp")
    private String otp;

    @SerializedName("full_name")
    private String fullName;

    public VerifyOtpSignUpRequest(String email, String otp, String fullName) {
        this.email = email;
        this.otp = otp;
        this.fullName = fullName;
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }

    public String getFullName() {
        return fullName;
    }
}