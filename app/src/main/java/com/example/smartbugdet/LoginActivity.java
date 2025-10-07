package com.example.smartbugdet;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.AuthResponse;
import com.example.smartbugdet.network.GenericMessageResponse;
import com.example.smartbugdet.network.GoogleIdTokenRequest;
import com.example.smartbugdet.network.OtpRequest;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.UserSetupStatusResponse; // Added
import com.example.smartbugdet.network.VerifyOtpLoginRequest;
import com.example.smartbugdet.util.AuthTokenManager;
import com.example.smartbugdet.util.CredentialManagerGoogleHelper;

import org.json.JSONException; // Added for error parsing
import org.json.JSONObject;   // Added for error parsing
import java.io.IOException;     // Added for error parsing

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements CredentialManagerGoogleHelper.GoogleSignInCallback {

    private static final String TAG = "LoginActivity";
    private static final int HTTP_NOT_FOUND_STATUS_CODE = 404; // For setup status

    private EditText etLoginEmail;
    private EditText etLoginOtp;
    private TextView tvLoginOtpLabel;
    private Button btnLoginOtp;
    private Button btnGoogleLogin;
    private TextView tvBottomSignup;
    private ProgressBar pbLoginLoading;

    private String serverClientId;
    private boolean isOtpSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Assuming your layout file is activity_login.xml or similar
        // If it's R.layout.activity_login_acitivity, make sure that file exists and is correct.
        // For this example, I'll assume R.layout.activity_login if activity_login_acitivity was a typo.
        // If R.layout.activity_login_acitivity is correct, no change needed here.
        setContentView(R.layout.activity_login_acitivity); // Verify this layout name
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etLoginEmail = findViewById(R.id.et_login_email);
        etLoginOtp = findViewById(R.id.et_login_otp);
        tvLoginOtpLabel = findViewById(R.id.tv_login_otp_label);
        btnLoginOtp = findViewById(R.id.btn_login_otp);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        tvBottomSignup = findViewById(R.id.tv_bottom_signup);
        pbLoginLoading = findViewById(R.id.pb_login_loading);

        serverClientId = getString(R.string.google_web_client_id);

        if (btnLoginOtp != null) {
            btnLoginOtp.setOnClickListener(view -> {
                if (!isOtpSent) {
                    handleSendOtpForLogin();
                } else {
                    handleVerifyOtpAndLogin();
                }
            });
        }

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                 if (!isOtpSent) {
                    signInWithGoogle();
                } else {
                    Toast.makeText(this, "Please complete OTP verification first.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (tvBottomSignup != null) {
            tvBottomSignup.setOnClickListener(view ->
                    startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
            );
        }
        updateUiForOtpState();
    }

    private void showLoading(boolean isLoading) {
        if (pbLoginLoading != null) {
            pbLoginLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (etLoginEmail != null) etLoginEmail.setEnabled(!isLoading && !isOtpSent);
        if (etLoginOtp != null) etLoginOtp.setEnabled(!isLoading && isOtpSent); // OTP field enabled only when OTP sent and not loading
        if (btnLoginOtp != null) btnLoginOtp.setEnabled(!isLoading);
        if (btnGoogleLogin != null) btnGoogleLogin.setEnabled(!isLoading && !isOtpSent);
        if (tvBottomSignup != null) tvBottomSignup.setEnabled(!isLoading);
    }

    private void updateUiForOtpState() {
        if (isOtpSent) {
            if (tvLoginOtpLabel != null) tvLoginOtpLabel.setVisibility(View.VISIBLE);
            if (etLoginOtp != null) etLoginOtp.setVisibility(View.VISIBLE);
            if (btnLoginOtp != null) btnLoginOtp.setText("Verify OTP & Login");
            if (etLoginEmail != null) etLoginEmail.setEnabled(false);
            if (btnGoogleLogin != null) {
                btnGoogleLogin.setEnabled(false);
                btnGoogleLogin.setAlpha(0.5f);
            }
        } else {
            if (tvLoginOtpLabel != null) tvLoginOtpLabel.setVisibility(View.GONE);
            if (etLoginOtp != null) {
                etLoginOtp.setVisibility(View.GONE);
                etLoginOtp.setText(""); // Clear OTP
            }
            if (btnLoginOtp != null) btnLoginOtp.setText("Send OTP");
            if (etLoginEmail != null) etLoginEmail.setEnabled(true);
            if (btnGoogleLogin != null) {
                btnGoogleLogin.setEnabled(true);
                btnGoogleLogin.setAlpha(1.0f);
            }
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
                if (errorJson.has("error")) { // Some APIs use "error"
                    return errorJson.optString("error", defaultMessage);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing error body JSON: " + errorBodyString, e);
        } catch (IOException e) {
            Log.e(TAG, "IOException reading error body: " + errorBodyString, e); // Log errorBodyString here too
        }
        // Fallback if parsing failed or no specific message found
        return defaultMessage + (response.message().isEmpty() ? "" : " (" + response.message() + ")");
    }


    private void handleSendOtpForLogin() {
        String email = etLoginEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Email is required");
            etLoginEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.setError("Enter a valid email address");
            etLoginEmail.requestFocus();
            return;
        }
        showLoading(true);
        OtpRequest otpRequest = new OtpRequest(email);
        ApiService apiService = RetrofitClient.getApiService();
        Call<GenericMessageResponse> call = apiService.sendOtp(otpRequest);
        call.enqueue(new Callback<GenericMessageResponse>() {
            @Override
            public void onResponse(Call<GenericMessageResponse> call, Response<GenericMessageResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    isOtpSent = true;
                    updateUiForOtpState();
                    if (etLoginOtp != null) etLoginOtp.requestFocus();
                } else {
                    // Use the generic error parser
                    String errorMessage = parseErrorMessage(response, "Failed to send OTP for login. Code: " + response.code());
                    Log.e(TAG, "Send OTP for login failed: " + errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<GenericMessageResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Send OTP for login request failed: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network error. Please check connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleVerifyOtpAndLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String otp = etLoginOtp.getText().toString().trim();
        if (TextUtils.isEmpty(otp)) {
            etLoginOtp.setError("OTP is required");
            etLoginOtp.requestFocus();
            return;
        }
        showLoading(true);
        VerifyOtpLoginRequest request = new VerifyOtpLoginRequest(email, otp);
        ApiService apiService = RetrofitClient.getApiService();
        Call<AuthResponse> call = apiService.verifyOtpAndLogin(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                // Do not hide loading here, it will be hidden after setup check
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "OTP Login successful. Message: " + authResponse.getMessage());
                    if (authResponse.getToken() != null && !authResponse.getToken().isEmpty()) {
                        AuthTokenManager.saveAuthToken(getApplicationContext(), authResponse.getToken());
                        Toast.makeText(LoginActivity.this, "Login successful. Checking setup...", Toast.LENGTH_SHORT).show();
                        checkSetupStatusAndNavigate(authResponse.getToken()); // New Step
                    } else {
                        showLoading(false); // Hide loading as flow stops here
                        Log.w(TAG, "Token not found in login response.");
                        Toast.makeText(LoginActivity.this, "Authentication token not received.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showLoading(false); // Hide loading on error
                    String errorMessage = parseErrorMessage(response, "Login failed. Code: " + response.code());
                     if (response.code() == 401) { // Unauthorized specifically
                        errorMessage = "Invalid OTP or email.";
                    }
                    Log.e(TAG, "Login failed: " + errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Login request failed: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network error. Please check connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        if (serverClientId == null || serverClientId.isEmpty() || serverClientId.equals("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")) {
            Toast.makeText(this, "Google Web Client ID not configured.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Google Web Client ID is not configured in strings.xml or is still the placeholder.");
            return;
        }
        showLoading(true);
        CredentialManagerGoogleHelper.signIn(this, serverClientId, this);
    }

    @Override
    public void onSuccess(String googleIdToken) {
        runOnUiThread(() -> {
            Log.d(TAG, "Google Sign-In via Credential Manager Success. ID Token: " + googleIdToken);
            // Toast.makeText(LoginActivity.this, "Google Sign-In successful. Verifying with server...", Toast.LENGTH_SHORT).show(); // Toast moved
            sendGoogleIdTokenToBackend(googleIdToken);
        });
    }

    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            showLoading(false);
            Log.e(TAG, "Google Sign-In via Credential Manager Error: " + errorMessage);
            Toast.makeText(LoginActivity.this, "Google Sign-In Failed: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void sendGoogleIdTokenToBackend(String googleIdToken) {
        ApiService apiService = RetrofitClient.getApiService();
        GoogleIdTokenRequest request = new GoogleIdTokenRequest(googleIdToken);
        Call<AuthResponse> call = apiService.verifyGoogleToken(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                // Do not hide loading here, it will be hidden after setup check
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "Backend Google token verification successful. Message: " + authResponse.getMessage());
                    if (authResponse.getToken() != null && !authResponse.getToken().isEmpty()) {
                        AuthTokenManager.saveAuthToken(getApplicationContext(), authResponse.getToken());
                        Toast.makeText(LoginActivity.this, "Google login successful. Checking setup...", Toast.LENGTH_LONG).show();
                        checkSetupStatusAndNavigate(authResponse.getToken()); // New Step
                    } else {
                        showLoading(false); // Hide loading as flow stops here
                        Log.w(TAG, "App token not found in backend response after Google Sign-In.");
                        Toast.makeText(LoginActivity.this, "Failed to get app token from server after Google Sign-In.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    showLoading(false); // Hide loading on error
                    String errorMessage = parseErrorMessage(response, "Backend Google token verification failed. Code: " + response.code());
                    Log.e(TAG, "Backend Google token verification failed: " + errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Backend Google token verification request failed: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network error during Google Sign-In backend verification.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkSetupStatusAndNavigate(String authToken) {
        // showLoading(true) should already be active from the previous step
        ApiService apiService = RetrofitClient.getApiService();
        Call<UserSetupStatusResponse> call = apiService.checkUserSetupStatus("Bearer " + authToken);

        call.enqueue(new Callback<UserSetupStatusResponse>() {
            @Override
            public void onResponse(Call<UserSetupStatusResponse> call, Response<UserSetupStatusResponse> response) {
                showLoading(false); // Now hide loading
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSetupComplete()) {
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Please complete your account setup.", Toast.LENGTH_SHORT).show();
                        navigateToUserSetupActivity();
                    }
                } else if (response.code() == HTTP_NOT_FOUND_STATUS_CODE) {
                    // User might exist via token, but no entry in setup table - treat as setup not complete
                    Log.w(TAG, "User setup status not found (404), proceeding to setup. Message: " + parseErrorMessage(response, ""));
                    Toast.makeText(LoginActivity.this, "Welcome! Let's get your account set up.", Toast.LENGTH_SHORT).show();
                    navigateToUserSetupActivity();
                }
                else {
                    // Other errors during setup check
                    String errorMsg = parseErrorMessage(response, "Failed to check user setup status. Code: " + response.code());
                    Log.e(TAG, "Error checking user setup: " + errorMsg);
                    Toast.makeText(LoginActivity.this, errorMsg + " Please try logging in again.", Toast.LENGTH_LONG).show();
                    // Optional: Clear token here? Or let user retry login.
                    // For now, user stays on LoginActivity.
                     isOtpSent = false; // Reset OTP state if there was an error after login success
                     updateUiForOtpState();
                }
            }

            @Override
            public void onFailure(Call<UserSetupStatusResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error checking user setup status: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network error checking setup. Please try logging in again.", Toast.LENGTH_LONG).show();
                 isOtpSent = false; // Reset OTP state
                 updateUiForOtpState();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToUserSetupActivity() {
        Intent intent = new Intent(LoginActivity.this, UserSetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
