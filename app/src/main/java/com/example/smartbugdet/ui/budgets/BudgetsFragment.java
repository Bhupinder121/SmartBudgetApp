package com.example.smartbugdet.ui.budgets;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartbugdet.R;
import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.SpendingLimitRequest;
import com.example.smartbugdet.util.AuthTokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BudgetsFragment extends Fragment {

    private TextView tvCurrentDailyLimit;
    private TextInputLayout tilNewDailyLimit;
    private TextInputEditText etNewDailyLimit;
    private MaterialButton btnSaveDailyLimit;
    private ProgressBar pbBudgetsLoading;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budgets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getApiService();

        tvCurrentDailyLimit = view.findViewById(R.id.tv_current_daily_limit);
        tilNewDailyLimit = view.findViewById(R.id.til_new_daily_limit);
        etNewDailyLimit = view.findViewById(R.id.et_new_daily_limit);
        btnSaveDailyLimit = view.findViewById(R.id.btn_save_daily_limit);
        pbBudgetsLoading = view.findViewById(R.id.pb_budgets_loading);

        btnSaveDailyLimit.setOnClickListener(v -> saveNewSpendingLimit());
    }

    private void saveNewSpendingLimit() {
        String newLimitStr = etNewDailyLimit.getText().toString().trim();

        if (TextUtils.isEmpty(newLimitStr)) {
            tilNewDailyLimit.setError("Please enter a new limit.");
            return;
        }

        double newLimit;
        try {
            newLimit = Double.parseDouble(newLimitStr);
            if (newLimit <= 0) {
                tilNewDailyLimit.setError("Limit must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            tilNewDailyLimit.setError("Invalid number format.");
            return;
        }

        tilNewDailyLimit.setError(null); // Clear any previous errors
        pbBudgetsLoading.setVisibility(View.VISIBLE);

        String authToken = AuthTokenManager.getAuthToken(getContext());
        if (authToken == null) {
            Toast.makeText(getContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
            pbBudgetsLoading.setVisibility(View.GONE);
            return;
        }

        SpendingLimitRequest request = new SpendingLimitRequest(newLimit);
        apiService.updateSpendingLimit("Bearer " + authToken, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                pbBudgetsLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Spending limit updated successfully!", Toast.LENGTH_SHORT).show();
                    // Optionally, update the tvCurrentDailyLimit TextView
                    tvCurrentDailyLimit.setText(String.format(java.util.Locale.getDefault(), "₹%.2f", newLimit));
                    etNewDailyLimit.setText(""); // Clear input field
                } else {
                    Toast.makeText(getContext(), "Failed to update limit. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                pbBudgetsLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "An error occurred: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
