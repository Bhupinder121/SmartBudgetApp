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
import com.example.smartbugdet.network.VerifyOtpSignUpRequest;
import com.example.smartbugdet.util.AuthTokenManager;
import com.example.smartbugdet.util.CredentialManagerGoogleHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity implements CredentialManagerGoogleHelper.GoogleSignInCallback {

    private static final String TAG = "SignUpActivity";
    private static final int HTTP_CONFLICT_STATUS_CODE = 409;

    private EditText etFullName;
    private EditText etEmail;
    private EditText etOtp;
    private TextView tvOtpLabel;
    private Button btnSignupOtp;
    private Button googleSignInButton;
    private ProgressBar pbSignupLoading;
    private TextView tvLoginRedirect;

    private String serverClientId;
    private boolean isOtpSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etOtp = findViewById(R.id.et_otp);
        tvOtpLabel = findViewById(R.id.tv_otp_label);
        btnSignupOtp = findViewById(R.id.btn_signup_otp);
        googleSignInButton = findViewById(R.id.google_signup_button);
        pbSignupLoading = findViewById(R.id.pb_signup_loading);
        tvLoginRedirect = findViewById(R.id.tv_bottom_login);

        if (tvLoginRedirect != null) {
            tvLoginRedirect.setOnClickListener(v -> navigateToLoginActivity());
        }

        serverClientId = getString(R.string.google_web_client_id);

        if (googleSignInButton != null) {
            googleSignInButton.setOnClickListener(v -> {
                if (!isOtpSent) {
                    signInWithGoogle();
                } else {
                    Toast.makeText(this, "Please complete OTP verification first or go back.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnSignupOtp != null) {
            btnSignupOtp.setOnClickListener(v -> {
                if (!isOtpSent) {
                    handleSendOtp();
                } else {
                    handleVerifyOtpAndSignUp();
                }
            });
        }
        updateUiForOtpState();
    }

    private void showLoading(boolean isLoading) {
        if (pbSignupLoading != null) {
            pbSignupLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        etFullName.setEnabled(!isLoading && !isOtpSent);
        etEmail.setEnabled(!isLoading && !isOtpSent);
        etOtp.setEnabled(!isLoading && isOtpSent);
        btnSignupOtp.setEnabled(!isLoading);
        googleSignInButton.setEnabled(!isLoading && !isOtpSent);
    }

    private void updateUiForOtpState() {
        if (isOtpSent) {
            tvOtpLabel.setVisibility(View.VISIBLE);
            etOtp.setVisibility(View.VISIBLE);
            etOtp.setEnabled(true);
            btnSignupOtp.setText("Verify OTP & Sign Up");
            etFullName.setEnabled(false);
            etFullName.setAlpha(0.5f);
            etEmail.setEnabled(false);
            etEmail.setAlpha(0.5f);
            googleSignInButton.setEnabled(false);
            googleSignInButton.setAlpha(0.5f);
        } else {
            tvOtpLabel.setVisibility(View.GONE);
            etOtp.setVisibility(View.GONE);
            etOtp.setEnabled(false);
            etOtp.setText("");
            btnSignupOtp.setText("Send OTP");
            etFullName.setEnabled(true);
            etFullName.setAlpha(1.0f);
            etEmail.setEnabled(true);
            etEmail.setAlpha(1.0f);
            etEmail.setError(null);
            etFullName.setError(null);
            googleSignInButton.setEnabled(true);
            googleSignInButton.setAlpha(1.0f);
        }
    }

    // Parses for {"message": "..."} or {"error": "..."}
    private String parseConflictOrErrorMessage(Response<?> response, String defaultMessage) {
        String errorBodyString = null;
        try {
            if (response.errorBody() != null) {
                errorBodyString = response.errorBody().string();
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
            Log.e(TAG, "IOException reading error body: ", e);
        }
        return defaultMessage;
    }

    private void handleSendOtp() {
        String fullName = etFullName.getText().toString().trim();
        String emailText = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(emailText)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        showLoading(true);
        OtpRequest otpRequest = new OtpRequest(emailText);
        ApiService apiService = RetrofitClient.getApiService();
        Call<GenericMessageResponse> call = apiService.sendOtp(otpRequest);

        call.enqueue(new Callback<GenericMessageResponse>() {
            @Override
            public void onResponse(Call<GenericMessageResponse> call, Response<GenericMessageResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    // New user: OTP sent for sign-up
                    Toast.makeText(SignUpActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    isOtpSent = true;
                    updateUiForOtpState();
                    etOtp.requestFocus();
                } else if (response.code() == HTTP_CONFLICT_STATUS_CODE) {
                    // Existing user detected at sendOtp stage.
                    // Server returns 409 WITHOUT a token here.
                    String conflictMessage = parseConflictOrErrorMessage(response, "An account with this email already exists. Please log in.");
                    Log.w(TAG, "Conflict (sendOtp): " + conflictMessage);
                    Toast.makeText(SignUpActivity.this, conflictMessage, Toast.LENGTH_LONG).show();
                    isOtpSent = false; // Reset: User should not proceed to OTP screen.
                    updateUiForOtpState(); // Reset UI, etEmail might get focus or user clicks login link.
                    etEmail.requestFocus();
                } else {
                    // Other errors (not 2xx, not 409)
                    String errorMessage = parseConflictOrErrorMessage(response, "Failed to send OTP. Code: " + response.code());
                    Log.e(TAG, "Send OTP failed: " + errorMessage + ", Code: " + response.code());
                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    isOtpSent = false;
                    updateUiForOtpState();
                }
            }

            @Override
            public void onFailure(Call<GenericMessageResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Send OTP request failed: " + t.getMessage(), t);
                Toast.makeText(SignUpActivity.this, "Network error. Please check connection.", Toast.LENGTH_LONG).show();
                 isOtpSent = false;
                 updateUiForOtpState();
            }
        });
    }

    private void handleVerifyOtpAndSignUp() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("OTP is required");
            etOtp.requestFocus();
            return;
        }

        showLoading(true);
        VerifyOtpSignUpRequest request = new VerifyOtpSignUpRequest(email, otp, fullName);
        ApiService apiService = RetrofitClient.getApiService();
        Call<AuthResponse> call = apiService.verifyOtpAndSignUp(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "OTP Verification & Sign Up successful. Message: " + authResponse.getMessage());
                    Toast.makeText(SignUpActivity.this, "Sign up successful: " + authResponse.getMessage(), Toast.LENGTH_LONG).show();

                    if (authResponse.getToken() != null && !authResponse.getToken().isEmpty()) {
                        AuthTokenManager.saveAuthToken(getApplicationContext(), authResponse.getToken());
                        // CHANGED: Navigate to UserSetupActivity for new sign-ups
                        navigateToUserSetupActivity();
                    } else {
                        Log.w(TAG, "Token not found in successful sign up response.");
                        Toast.makeText(SignUpActivity.this, "Authentication token not received after sign up.", Toast.LENGTH_SHORT).show();
                        isOtpSent = false;
                        updateUiForOtpState();
                    }
                } else if (response.code() == HTTP_CONFLICT_STATUS_CODE) {
                    // Existing user detected at OTP verification stage, server returns 409 WITH token here.
                    String errorBodyString = null;
                    try {
                        if (response.errorBody() != null) {
                            errorBodyString = response.errorBody().string();
                            JSONObject conflictJson = new JSONObject(errorBodyString);
                            String serverMessage = conflictJson.optString("message", "An account with this email already exists. Logging you in...");
                            String token = conflictJson.optString("token", null);

                            if (token != null && !token.isEmpty()) {
                                Toast.makeText(SignUpActivity.this, serverMessage, Toast.LENGTH_LONG).show();
                                AuthTokenManager.saveAuthToken(getApplicationContext(), token);
                                // Existing user logged in, go to MainActivity (assuming setup was done)
                                navigateToMainActivity();
                            } else {
                                Log.e(TAG, "Conflict (verifyOtpAndSignUp) response did not contain a token. Body: " + errorBodyString);
                                String displayMessage = conflictJson.has("message") ? serverMessage : "Account exists, but failed to log in automatically. Please use the Login page.";
                                Toast.makeText(SignUpActivity.this, displayMessage, Toast.LENGTH_LONG).show();
                                isOtpSent = false; // Reset UI
                                updateUiForOtpState();
                            }
                        } else {
                             Toast.makeText(SignUpActivity.this, "An account with this email already exists. Please log in.", Toast.LENGTH_LONG).show();
                             isOtpSent = false; // Reset UI
                             updateUiForOtpState();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing 409 (verifyOtpAndSignUp) JSON: " + errorBodyString, e);
                        Toast.makeText(SignUpActivity.this, "Error processing server response. Please try logging in.", Toast.LENGTH_LONG).show();
                        isOtpSent = false; // Reset UI
                        updateUiForOtpState();
                    } catch (IOException e) {
                        Log.e(TAG, "IOException reading 409 (verifyOtpAndSignUp) error body: ", e);
                        Toast.makeText(SignUpActivity.this, "Error reading server response. Please try logging in.", Toast.LENGTH_LONG).show();
                        isOtpSent = false; // Reset UI
                        updateUiForOtpState();
                    }
                } else {
                    // Other errors during OTP verification for sign-up
                    String defaultMessage = "Sign up failed after OTP. Code: " + response.code();
                    String serverMessage = parseConflictOrErrorMessage(response, defaultMessage);
                    Log.e(TAG, "Sign up failed: " + serverMessage + ", Code: " + response.code());
                    Toast.makeText(SignUpActivity.this, serverMessage, Toast.LENGTH_LONG).show();
                    isOtpSent = false;
                    updateUiForOtpState();
                    etEmail.requestFocus();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Sign up request failed: " + t.getMessage(), t);
                Toast.makeText(SignUpActivity.this, "Network error. Please check connection.", Toast.LENGTH_LONG).show();
                isOtpSent = false;
                updateUiForOtpState();
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
        if (isOtpSent) {
             isOtpSent = false;
             updateUiForOtpState();
        }
        CredentialManagerGoogleHelper.signIn(this, serverClientId, this);
    }

    @Override
    public void onSuccess(String googleIdToken) {
        runOnUiThread(() -> {
            Log.d(TAG, "Credential Manager Google Sign-In Success. ID Token: " + googleIdToken);
            Toast.makeText(SignUpActivity.this, "Google Sign-In successful. Verifying with server...", Toast.LENGTH_SHORT).show();
            sendGoogleIdTokenToBackend(googleIdToken);
        });
    }

    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            showLoading(false);
            Log.e(TAG, "Credential Manager Google Sign-In Error: " + errorMessage);
            Toast.makeText(SignUpActivity.this, "Google Sign-In Failed: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void sendGoogleIdTokenToBackend(String googleIdToken) {
        ApiService apiService = RetrofitClient.getApiService();
        GoogleIdTokenRequest request = new GoogleIdTokenRequest(googleIdToken);

        Call<AuthResponse> call = apiService.verifyGoogleToken(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "Backend verification successful (Google). Message: " + authResponse.getMessage());
                    if (authResponse.getToken() != null && !authResponse.getToken().isEmpty()) {
                        AuthTokenManager.saveAuthToken(getApplicationContext(), authResponse.getToken());
                        // CHANGED: Navigate to UserSetupActivity for new Google sign-ups/logins that are treated as new
                        // Backend should ideally differentiate new vs existing Google users
                        // For now, if /auth/google is used from SignUp, we assume it's for a setup flow.
                        navigateToUserSetupActivity();
                    } else {
                        Log.w(TAG, "Token not found in Google Sign-In backend response.");
                        Toast.makeText(SignUpActivity.this, "Failed to get app token from server after Google Sign-In.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    String defaultMessage = "Backend verification failed (Google). Code: " + response.code();
                    String serverMessage = parseConflictOrErrorMessage(response, defaultMessage);
                     if (response.code() == HTTP_CONFLICT_STATUS_CODE) {
                         // If Google Sign-In from SignUpActivity results in a conflict,
                         // it implies the user might exist.
                         // The ideal here is that /auth/google might return a flag if setup is needed.
                         // For now, let's assume they might need setup OR login.
                         // Redirecting to Login to handle it might be safer, or show specific message.
                         serverMessage = parseConflictOrErrorMessage(response, "An account with this Google email might already exist. Try logging in with Google on the Login page.");
                    }
                    Log.e(TAG, "Backend verification failed (Google): " + serverMessage + ", Code: " + response.code());
                    Toast.makeText(SignUpActivity.this, serverMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Backend verification request failed (Google): " + t.getMessage(), t);
                Toast.makeText(SignUpActivity.this, "Network error during Google Sign-In backend verification.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // New method to navigate to UserSetupActivity
    private void navigateToUserSetupActivity() {
        Intent intent = new Intent(SignUpActivity.this, UserSetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

     private void navigateToLoginActivity() {
         Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
         startActivity(intent);
         // finish(); // Typically, you don't finish SignUpActivity when going to Login, so user can go back
     }
}
