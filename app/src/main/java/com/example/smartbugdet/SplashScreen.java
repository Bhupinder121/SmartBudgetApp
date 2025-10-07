package com.example.smartbugdet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.GenericMessageResponse;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.UserSetupStatusResponse; // Import needed
import com.example.smartbugdet.util.AuthTokenManager;

import org.json.JSONException; // For error parsing
import org.json.JSONObject;   // For error parsing
import java.io.IOException;     // For error parsing

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashScreen extends AppCompatActivity {

    Button btn_create_account;
    TextView btn_login;
    private static final String TAG = "SplashScreen";
    private static final int HTTP_NOT_FOUND_STATUS_CODE = 404;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_create_account = findViewById(R.id.btn_create_account);
        btn_login = findViewById(R.id.tv_login);

        // Initially hide buttons, they will be shown if needed
        btn_create_account.setVisibility(View.GONE);
        btn_login.setVisibility(View.GONE);

        btn_create_account.setOnClickListener(v -> {
            Intent intent = new Intent(SplashScreen.this, SignUpActivity.class);
            startActivity(intent);
        });

        btn_login.setOnClickListener(v -> {
            Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
            startActivity(intent);
        });

        new Handler(Looper.getMainLooper()).postDelayed(this::checkTokenAndProceed, 500); // Renamed for clarity

    }

    private void checkTokenAndProceed() {
        String authToken = AuthTokenManager.getAuthToken(this);

        if (authToken == null || authToken.isEmpty()) {
            Log.i(TAG, "No auth token found. Showing login/signup options.");
            showLoginSignupButtons();
        } else {
            Log.i(TAG, "Auth token found. Verifying with server then checking setup status.");
            // Instead of just verifying, we directly try to get setup status.
            // If the token is invalid, /user/setup should fail (e.g. 401).
            fetchUserSetupStatus(authToken);
        }
    }

    // Helper to parse generic error messages
    private String parseErrorMessage(Response<?> response, String defaultMessage) {
        String errorBodyString = null;
        try {
            if (response.errorBody() != null) {
                errorBodyString = response.errorBody().string(); // Read once
                JSONObject errorJson = new JSONObject(errorBodyString);
                if (errorJson.has("message")) {
                    return errorJson.optString("message", defaultMessage);
                }
                if (errorJson.has("error")) {
                    return errorJson.optString("error", defaultMessage);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing error body JSON: " + errorBodyString, e);
        } catch (IOException e) {
            Log.e(TAG, "IOException reading error body: " + errorBodyString, e);
        }
        return defaultMessage + (response.message() != null && !response.message().isEmpty() ? " (" + response.message() + ")" : "");
    }


    private void fetchUserSetupStatus(String authToken) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<UserSetupStatusResponse> call = apiService.checkUserSetupStatus("Bearer " + authToken);

        call.enqueue(new Callback<UserSetupStatusResponse>() {
            @Override
            public void onResponse(Call<UserSetupStatusResponse> call, Response<UserSetupStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSetupComplete()) {
                        Log.i(TAG, "User setup is complete. Navigating to Home.");
                        navigateToHome();
                    } else {
                        Log.i(TAG, "User setup is NOT complete. Navigating to UserSetupActivity.");
                        navigateToUserSetupActivity();
                    }
                } else if (response.code() == HTTP_NOT_FOUND_STATUS_CODE) {
                    // API explicitly states setup_complete: false or user not found, implying setup needed
                    Log.w(TAG, "User setup status not found (404), proceeding to setup. Message: " + parseErrorMessage(response,""));
                    navigateToUserSetupActivity();
                } else if (response.code() == 401 || response.code() == 403) { // Unauthorized or Forbidden
                    Log.w(TAG, "Token invalid or expired when checking setup status. Code: " + response.code());
                    Toast.makeText(SplashScreen.this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                    AuthTokenManager.clearAuthToken(SplashScreen.this);
                    showLoginSignupButtons();
                }
                else {
                    // Other errors during setup check
                    String errorMsg = parseErrorMessage(response, "Failed to check user setup status. Code: " + response.code());
                    Log.e(TAG, "Error checking user setup: " + errorMsg);
                    Toast.makeText(SplashScreen.this, "Could not verify session. Please try logging in.", Toast.LENGTH_LONG).show();
                    AuthTokenManager.clearAuthToken(SplashScreen.this); // Clear token on unexpected errors too
                    showLoginSignupButtons();
                }
            }

            @Override
            public void onFailure(Call<UserSetupStatusResponse> call, Throwable t) {
                Log.e(TAG, "Network error checking user setup status: " + t.getMessage(), t);
                Toast.makeText(SplashScreen.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
                // Depending on desired UX, could show retry or just go to login/signup
                AuthTokenManager.clearAuthToken(SplashScreen.this); // Clear token on failure
                showLoginSignupButtons();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(SplashScreen.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToUserSetupActivity() {
        Intent intent = new Intent(SplashScreen.this, UserSetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
        // Do not finish SplashScreen here if LoginActivity can lead back or if you want SplashScreen visible briefly
        startActivity(intent);
        finish(); // Finish SplashScreen so user cannot go back to it from Login/SignUp
    }


    private void showLoginSignupButtons(){
        // Ensure this is called on the UI thread if coming from a background thread, though Handler should handle it.
        btn_create_account.setVisibility(View.VISIBLE);
        btn_login.setVisibility(View.VISIBLE);
        // Optionally hide a loading indicator if one was shown
    }
}
