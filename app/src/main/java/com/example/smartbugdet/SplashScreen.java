package com.example.smartbugdet;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.UserSetupStatusResponse;
import com.example.smartbugdet.util.AuthTokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashScreen extends AppCompatActivity {

    private static final String TAG = "SplashScreen";
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    private Button btn_create_account;
    private TextView btn_login;
    private ApiService apiService;
    private boolean isUserSetupComplete = false;

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

        apiService = RetrofitClient.getApiService();
        btn_create_account = findViewById(R.id.btn_create_account);
        btn_login = findViewById(R.id.tv_login);

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

        new Handler(Looper.getMainLooper()).postDelayed(this::checkTokenAndProceed, 500);
    }

    private void checkTokenAndProceed() {
        String authToken = AuthTokenManager.getAuthToken(this);
        if (authToken == null || authToken.isEmpty()) {
            showLoginSignupButtons();
        } else {
            fetchUserSetupStatus(authToken);
        }
    }

    private void fetchUserSetupStatus(String authToken) {
        apiService.checkUserSetupStatus("Bearer " + authToken).enqueue(new Callback<UserSetupStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserSetupStatusResponse> call, @NonNull Response<UserSetupStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isUserSetupComplete = response.body().isSetupComplete();
                    requestSmsPermissionThenProceed();
                } else {
                    handleSetupCheckFailure(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserSetupStatusResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error checking user setup: " + t.getMessage(), t);
                Toast.makeText(SplashScreen.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
                showLoginSignupButtons();
            }
        });
    }

    private void handleSetupCheckFailure(Response<UserSetupStatusResponse> response) {
        if (response.code() == 401 || response.code() == 403) {
            Toast.makeText(SplashScreen.this, "Session expired. Please log in.", Toast.LENGTH_SHORT).show();
            AuthTokenManager.clearAuthToken(SplashScreen.this);
        }
        showLoginSignupButtons();
    }

    private void requestSmsPermissionThenProceed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
             AlertDialog dialog =  new AlertDialog.Builder(this)
                    .setTitle("SMS Permission for Automated Tracking")
                    .setMessage("To help you track finances automatically, our app can read transaction SMS. This is optional but recommended. Your data is processed securely on your device.")
                    .setPositiveButton("Grant Permission", (d, which) -> {
                        ActivityCompat.requestPermissions(SplashScreen.this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Maybe Later", (d, which) -> {
                        Log.i(TAG, "SMS permission deferred by user.");
                        proceedToNextScreen();
                    })
                    .setCancelable(false)
                    .create();
             dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.active_nav_icon_tint));

        } else {
            Log.i(TAG, "SMS permission already granted.");
            proceedToNextScreen();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. You can grant it later from settings.", Toast.LENGTH_LONG).show();
            }
            proceedToNextScreen();
        }
    }

    private void proceedToNextScreen() {
        if (isUserSetupComplete) {
            navigateToHome();
        } else {
            navigateToUserSetup();
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToUserSetup() {
        Intent intent = new Intent(this, UserSetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoginSignupButtons() {
        btn_create_account.setVisibility(View.VISIBLE);
        btn_login.setVisibility(View.VISIBLE);
    }
}
