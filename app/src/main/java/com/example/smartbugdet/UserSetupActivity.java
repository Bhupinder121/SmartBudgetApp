package com.example.smartbugdet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.GenericMessageResponse;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.UserMetadataRequest;
import com.example.smartbugdet.ui.usersetup.WelcomeScreenFragment;
import com.example.smartbugdet.ui.usersetup.StartingBalanceFragment;
import com.example.smartbugdet.ui.usersetup.SpendingLimitFragment;
import com.example.smartbugdet.ui.usersetup.ConfirmationScreenFragment;
import com.example.smartbugdet.util.AuthTokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserSetupActivity extends AppCompatActivity {

    private static final String TAG = "UserSetupActivity";

    // Variables to store collected data
    private double startingBalance = -1; // Default to an invalid state
    private double dailySpendingLimit = -1; // Default to an invalid state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        if (savedInstanceState == null) {
            // Load the first fragment (Welcome Screen)
            loadFragment(new WelcomeScreenFragment(), false);
        }
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fcv_user_setup_fragment_container, fragment);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null); // Optional: Add to back stack if you want back navigation between setup steps
        }
        fragmentTransaction.commit();
    }

    // Navigation methods to be called by fragments
    public void navigateToStartingBalance() {
        loadFragment(new StartingBalanceFragment(), true);
    }

    public void navigateToSpendingLimit(double balance) {
        this.startingBalance = balance;
        Log.d(TAG, "Starting Balance collected: " + this.startingBalance);
        loadFragment(new SpendingLimitFragment(), true);
    }

    public void navigateToConfirmation(double limit) {
        this.dailySpendingLimit = limit;
        Log.d(TAG, "Daily Spending Limit collected: " + this.dailySpendingLimit);
        loadFragment(new ConfirmationScreenFragment(), true);
    }

    public void finishUserSetup() {
        Log.d(TAG, "User setup finished. Collected Balance: " + startingBalance + ", Limit: " + dailySpendingLimit);
        // Here, make the API call to save user metadata
        saveUserMetadata();
    }

    private void saveUserMetadata() {
        String authToken = AuthTokenManager.getAuthToken(this);
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            // Optionally redirect to LoginActivity
            return;
        }

        if (startingBalance < 0 || dailySpendingLimit < 0) {
            Toast.makeText(this, "Setup data is incomplete. Cannot save.", Toast.LENGTH_LONG).show();
            // This case should ideally be prevented by fragment validation
            return;
        }

        UserMetadataRequest request = new UserMetadataRequest(startingBalance, dailySpendingLimit);
        ApiService apiService = RetrofitClient.getApiService();
        Call<GenericMessageResponse> call = apiService.saveUserMetadata("Bearer " + authToken, request);

        // Show loading indicator if you have one
        // findViewById(R.id.pb_user_setup_loading).setVisibility(View.VISIBLE);

        call.enqueue(new Callback<GenericMessageResponse>() {
            @Override
            public void onResponse(Call<GenericMessageResponse> call, Response<GenericMessageResponse> response) {
                // findViewById(R.id.pb_user_setup_loading).setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UserSetupActivity.this, "Setup data saved: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    proceedToMainActivity();
                } else {
                    String errorMsg = "Failed to save setup data.";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " Error: " + response.errorBody().string();
                        } catch (Exception e) { Log.e(TAG, "Error parsing error body", e); }
                    } else {
                        errorMsg += " Code: " + response.code();
                    }
                    Toast.makeText(UserSetupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    // Optional: Allow user to retry or go back
                }
            }

            @Override
            public void onFailure(Call<GenericMessageResponse> call, Throwable t) {
                // findViewById(R.id.pb_user_setup_loading).setVisibility(View.GONE);
                Log.e(TAG, "Save user metadata failed", t);
                Toast.makeText(UserSetupActivity.this, "Network error. Could not save setup data.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void proceedToMainActivity() {
        Intent intent = new Intent(UserSetupActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish UserSetupActivity
    }

    // Getters for fragments to potentially access stored data (though direct calls are used above)
    public double getStartingBalance() {
        return startingBalance;
    }

    public double getDailySpendingLimit() {
        return dailySpendingLimit;
    }
}