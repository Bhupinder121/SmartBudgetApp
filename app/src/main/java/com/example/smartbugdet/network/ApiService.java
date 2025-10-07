package com.example.smartbugdet.network;

import com.example.smartbugdet.model.Transaction;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // POST request to send Google ID token to backend and get app's JWT
    @POST("auth/google")
    Call<AuthResponse> verifyGoogleToken(@Body GoogleIdTokenRequest googleIdTokenRequest);

    // POST request to ask the backend to send an OTP to the user's email
    @POST("auth/send-otp")
    Call<GenericMessageResponse> sendOtp(@Body OtpRequest otpRequest);

    // POST request to verify OTP and complete user sign-up
    @POST("auth/verify-otp-signup")
    Call<AuthResponse> verifyOtpAndSignUp(@Body VerifyOtpSignUpRequest verifyOtpSignUpRequest);

    // POST request to verify OTP and complete user login
    @POST("auth/verify-otp-login")
    Call<AuthResponse> verifyOtpAndLogin(@Body VerifyOtpLoginRequest verifyOtpLoginRequest);

    // GET request to verify existing token
    @GET("verify/token")
    Call<GenericMessageResponse> verifyToken(@Header("Authorization") String authToken);

    // POST request to send a new transaction
    @POST("add/transaction")
    Call<GenericMessageResponse> sendTransaction(@Header("Authorization") String authToken, @Body TransactionRequest transactionRequest);

    // GET request to fetch today's transactions
    @GET("/transactions/today")
    Call<TodaysTransactionsResponse> getTodaysTransactions(@Header("Authorization") String authToken);

    // POST request to save user's initial setup data (metadata)
    @POST("/add/user/metadata")
    Call<GenericMessageResponse> saveUserMetadata(@Header("Authorization") String authToken, @Body UserMetadataRequest metadataRequest);

    // GET request to check if user has completed initial setup
    @GET("user/setup")
    Call<UserSetupStatusResponse> checkUserSetupStatus(@Header("Authorization") String authToken);

    // GET request to fetch user's home screen summary data
    @GET("user/home-summary")
    Call<List<HomeSummaryResponse>> getHomeSummaryData(@Header("Authorization") String authToken);

    // GET request to fetch user information (full name, email)
    @GET("user/info")
    Call<UserInfoResponse> getUserInfo(@Header("Authorization") String authToken);

    // GET request to fetch all transactions for the user
    @GET("user/transactions")
    Call<AllTransactionsResponse> getAllTransactions(@Header("Authorization") String authToken);

    @POST("/update/spending-limit")
    Call<ResponseBody> updateSpendingLimit(@Header("Authorization") String authToken, @Body SpendingLimitRequest request);
}
