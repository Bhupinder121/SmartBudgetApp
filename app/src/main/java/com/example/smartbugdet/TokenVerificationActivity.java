package com.example.smartbugdet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.GenericMessageResponse; // Assuming a generic response for success
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.util.AuthTokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TokenVerificationActivity extends AppCompatActivity {

    private static final String TAG = "TokenVerification";
    private ProgressBar pbVerification;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_token_verification);
        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pbVerification = findViewById(R.id.pb_token_verification);
        tvStatus = findViewById(R.id.tv_verifying_status);

        // A small delay to show the splash/verification screen briefly, can be adjusted or removed
        new Handler(Looper.getMainLooper()).postDelayed(this::checkTokenAndVerify, 500);
    }

    private void checkTokenAndVerify() {
        String authToken = AuthTokenManager.getAuthToken(this);

        if (authToken == null || authToken.isEmpty()) {
            Log.i(TAG, "No auth token found. Navigating to LoginActivity.");
            navigateToLogin();
        } else {
            Log.i(TAG, "Auth token found. Verifying with server.");
            pbVerification.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Verifying session...");
            verifyTokenWithServer(authToken);
        }
    }

    private void verifyTokenWithServer(String authToken) {
        ApiService apiService = RetrofitClient.getApiService();
        // The token is typically sent as "Bearer <token>"
        Call<GenericMessageResponse> call = apiService.verifyToken("Bearer " + authToken);

        call.enqueue(new Callback<GenericMessageResponse>() {
            @Override
            public void onResponse(Call<GenericMessageResponse> call, Response<GenericMessageResponse> response) {
                pbVerification.setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    // Assuming 200 OK means token is valid
                    Log.i(TAG, "Token verification successful. Navigating to Home.");
                    // Optional: Show a brief success message from server if needed
                    // if (response.body() != null && response.body().getMessage() != null) {
                    //    Toast.makeText(TokenVerificationActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    // }
                    navigateToHome();
                } else {
                    // Token is invalid (e.g., 401, 403) or other server error
                    Log.w(TAG, "Token verification failed. Code: " + response.code() + ". Clearing token and navigating to Login.");
                    Toast.makeText(TokenVerificationActivity.this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                    AuthTokenManager.clearAuthToken(TokenVerificationActivity.this);
                    navigateToLogin();
                }
            }

            @Override
            public void onFailure(Call<GenericMessageResponse> call, Throwable t) {
                pbVerification.setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                Log.e(TAG, "Token verification request failed: " + t.getMessage(), t);
                Toast.makeText(TokenVerificationActivity.this, "Network error. Please try logging in.", Toast.LENGTH_LONG).show();
                AuthTokenManager.clearAuthToken(TokenVerificationActivity.this);
                navigateToLogin();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(TokenVerificationActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(TokenVerificationActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
