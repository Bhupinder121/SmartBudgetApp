package com.example.smartbugdet.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbugdet.LoginActivity;
import com.example.smartbugdet.R;
import com.example.smartbugdet.model.Transaction;
import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.HomeSummaryResponse;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.TodaysTransactionsResponse;
import com.example.smartbugdet.network.UserInfoResponse;
import com.example.smartbugdet.network.User;
import com.example.smartbugdet.util.AuthTokenManager;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException; // Added for getCurrentTotalBalance
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView rvTodaysTransactions;
    private HomeTransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private TextView tvNoTransactions;
    private ProgressBar pbHomeLoading;

    private TextView tvUsernameGreeting;
    private TextView tvTotalBalanceValue;
    private TextView tvIncomeValue;
    private TextView tvExpenseValue;
    private TextView tvDailySpendingSummary;

    private View limitBar1, limitBar2, limitBar3, limitBar4;
    private TextView tvLimitLabel1, tvLimitLabel2, tvLimitLabel3, tvLimitLabel4;

    private boolean isSummaryLoading = false;
    private boolean isTransactionsLoading = false;
    private boolean isUserInfoLoading = false;

    private NumberFormat currencyFormatter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        tvUsernameGreeting = view.findViewById(R.id.username);
        tvTotalBalanceValue = view.findViewById(R.id.tv_total_balance_value);
        tvIncomeValue = view.findViewById(R.id.tv_income_value);
        tvExpenseValue = view.findViewById(R.id.tv_expense_value);
        tvDailySpendingSummary = view.findViewById(R.id.tv_daily_spending_summary);

        rvTodaysTransactions = view.findViewById(R.id.rv_todays_transactions);
        tvNoTransactions = view.findViewById(R.id.tv_no_transactions_home);
        pbHomeLoading = view.findViewById(R.id.pb_home_transactions_loading);

        limitBar1 = view.findViewById(R.id.limit_bar_1);
        limitBar2 = view.findViewById(R.id.limit_bar_2);
        limitBar3 = view.findViewById(R.id.limit_bar_3);
        limitBar4 = view.findViewById(R.id.limit_bar_4);
        tvLimitLabel1 = view.findViewById(R.id.tv_limit_label_1);
        tvLimitLabel2 = view.findViewById(R.id.tv_limit_label_2);
        tvLimitLabel3 = view.findViewById(R.id.tv_limit_label_3);
        tvLimitLabel4 = view.findViewById(R.id.tv_limit_label_4);

        rvTodaysTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        transactionAdapter = new HomeTransactionAdapter(getContext(), transactionList);
        rvTodaysTransactions.setAdapter(transactionAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAllHomeData();
    }

    private void fetchAllHomeData() {
        if (getContext() == null) return;
        String authToken = AuthTokenManager.getAuthToken(getContext());

        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(getContext(), "Please log in to view data.", Toast.LENGTH_SHORT).show();
            handleLogout();
            return;
        }
        setSummaryPlaceholders("Loading..."); // Reset UI before all fetches
        fetchUserInfo(authToken);
        fetchHomeSummaryData(authToken);
        fetchTodaysTransactions(authToken);
    }

    private void setSummaryPlaceholders(String placeholderText) {
        if (tvUsernameGreeting != null) tvUsernameGreeting.setText("Welcome Back!");
        if (tvTotalBalanceValue != null) tvTotalBalanceValue.setText(placeholderText);
        if (tvIncomeValue != null) tvIncomeValue.setText(placeholderText);
        if (tvExpenseValue != null) tvExpenseValue.setText(placeholderText);
        if (tvDailySpendingSummary != null) tvDailySpendingSummary.setText(placeholderText);
        if (isAdded() && getContext() != null) {
            updateSpendingLimitUI(0, 0); // Reset bars
        }
    }

    private void updateLoadingIndicatorVisibility() {
        if (!isAdded() || getContext() == null || pbHomeLoading == null) return;
        pbHomeLoading.setVisibility(isSummaryLoading || isTransactionsLoading || isUserInfoLoading ? View.VISIBLE : View.GONE);
    }

    private String parseErrorBody(Response<?> response) {
        String errorMessage = "Error: " + response.code();
        if (response.errorBody() != null) {
            try {
                errorMessage = response.errorBody().string();
            } catch (IOException e) {
                Log.e(TAG, "Error parsing error body", e);
                errorMessage = "Error reading response. Code: " + response.code();
            }
        } else if (response.message() != null && !response.message().isEmpty()) {
            errorMessage = response.message();
        }
        return errorMessage;
    }

    private void fetchUserInfo(String authToken) {
        if (getContext() == null) return;
        isUserInfoLoading = true;
        updateLoadingIndicatorVisibility();

        RetrofitClient.getApiService().getUserInfo("Bearer " + authToken).enqueue(new Callback<UserInfoResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserInfoResponse> call, @NonNull Response<UserInfoResponse> response) {
                isUserInfoLoading = false;
                updateLoadingIndicatorVisibility();
                if (!isAdded() || getContext() == null || tvUsernameGreeting == null) return;

                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    User user = response.body().getUser();
                    String fullName = user.getFullName();
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        String firstName = fullName.split(" ")[0];
                        tvUsernameGreeting.setText("Welcome Back, " + firstName + "!");
                    } else {
                        tvUsernameGreeting.setText("Welcome Back, User!");
                    }
                } else if (response.code() == 401) {
                    Log.w(TAG, "User info call failed due to auth: " + response.code());
                    if (tvUsernameGreeting != null) tvUsernameGreeting.setText("Welcome Back!");
                    // Full logout is handled by other calls if they also fail with 401
                } else {
                    Log.e(TAG, "Failed to fetch user info. " + parseErrorBody(response));
                    tvUsernameGreeting.setText("Welcome Back!");
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserInfoResponse> call, @NonNull Throwable t) {
                isUserInfoLoading = false;
                updateLoadingIndicatorVisibility();
                if (!isAdded() || getContext() == null || tvUsernameGreeting == null) return;
                Log.e(TAG, "Fetch user info failed: " + t.getMessage(), t);
                tvUsernameGreeting.setText("Welcome Back!");
            }
        });
    }

    private void fetchHomeSummaryData(String authToken) {
        if (getContext() == null) return;
        isSummaryLoading = true;
        updateLoadingIndicatorVisibility();

        RetrofitClient.getApiService().getHomeSummaryData("Bearer " + authToken).enqueue(new Callback<List<HomeSummaryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<HomeSummaryResponse>> call, @NonNull Response<List<HomeSummaryResponse>> response) {
                isSummaryLoading = false;
                updateLoadingIndicatorVisibility();
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    HomeSummaryResponse summary = response.body().get(0);
                    if (summary != null) {
                        tvTotalBalanceValue.setText(currencyFormatter.format(summary.getTotalBalance()));
                        tvIncomeValue.setText(currencyFormatter.format(summary.getIncomeToday()));
                        tvExpenseValue.setText(currencyFormatter.format(summary.getExpenseToday()));
                        String dailySummaryText = String.format(Locale.getDefault(), "%s spent of %s daily limit",
                                currencyFormatter.format(summary.getExpenseToday()),
                                currencyFormatter.format(summary.getDailySpendingLimit()));
                        tvDailySpendingSummary.setText(dailySummaryText);
                        updateSpendingLimitUI(summary.getExpenseToday(), summary.getDailySpendingLimit());
                    } else {
                        Log.e(TAG, "Home summary data was null despite successful response.");
                        setSummaryPlaceholders("N/A - Data Error");
                        Toast.makeText(getContext(), "Failed to load summary: Data format error.", Toast.LENGTH_LONG).show();
                    }
                } else if (response.isSuccessful() && (response.body() == null || response.body().isEmpty())) {
                    Log.w(TAG, "Home summary data is empty or null.");
                    setSummaryPlaceholders("N/A"); // Sets all placeholders including resetting bars
                }
                 else if (response.code() == 401) {
                    Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    handleLogout();
                } else {
                    String errorMsg = parseErrorBody(response);
                    Log.e(TAG, "Failed to fetch home summary. " + errorMsg);
                    Toast.makeText(getContext(), "Failed to load summary: " + errorMsg, Toast.LENGTH_LONG).show();
                    setSummaryPlaceholders("N/A - Error"); // Sets all placeholders including resetting bars
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HomeSummaryResponse>> call, @NonNull Throwable t) {
                isSummaryLoading = false;
                updateLoadingIndicatorVisibility();
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Fetch home summary failed: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error loading summary. Check connection.", Toast.LENGTH_LONG).show();
                setSummaryPlaceholders("Error"); // Sets all placeholders including resetting bars
            }
        });
    }

    private void updateSpendingLimitUI(double expenseToday, double dailySpendingLimit) {
        if (!isAdded() || getContext() == null || limitBar1 == null) return;

        expenseToday = Math.max(0, expenseToday);
        double percentageSpent = 0;
        if (dailySpendingLimit > 0) {
            percentageSpent = expenseToday / dailySpendingLimit;
        } else if (expenseToday > 0) {
            percentageSpent = 1.01; // Treat as > 100%
        }

        int colorInactive = ContextCompat.getColor(requireContext(), R.color.limit_bar_inactive_color);
        int colorHealthyActive = ContextCompat.getColor(requireContext(), R.color.limit_bar_healthy_active_color);
        int colorManageableActive = ContextCompat.getColor(requireContext(), R.color.limit_bar_manageable_active_color);
        int colorShouldStopActive = ContextCompat.getColor(requireContext(), R.color.limit_bar_should_stop_active_color);
        int colorOverLimitActive = ContextCompat.getColor(requireContext(), R.color.limit_bar_over_limit_active_color);
        int labelColorInactive = ContextCompat.getColor(requireContext(), R.color.limit_label_inactive_color);
        int labelColorActive = ContextCompat.getColor(requireContext(), R.color.limit_label_active_color);

        limitBar1.setBackgroundColor(percentageSpent > 0 ? colorHealthyActive : colorInactive);
        limitBar2.setBackgroundColor(percentageSpent > 0.25 ? colorManageableActive : colorInactive);
        limitBar3.setBackgroundColor(percentageSpent > 0.50 ? colorShouldStopActive : colorInactive);
        limitBar4.setBackgroundColor(percentageSpent > 0.75 ? colorOverLimitActive : colorInactive);

        tvLimitLabel1.setTextColor(labelColorInactive);
        tvLimitLabel2.setTextColor(labelColorInactive);
        tvLimitLabel3.setTextColor(labelColorInactive);
        tvLimitLabel4.setTextColor(labelColorInactive);

        if (percentageSpent > 0.75) tvLimitLabel4.setTextColor(labelColorActive);
        else if (percentageSpent > 0.50) tvLimitLabel3.setTextColor(labelColorActive);
        else if (percentageSpent > 0.25) tvLimitLabel2.setTextColor(labelColorActive);
        else if (percentageSpent > 0) tvLimitLabel1.setTextColor(labelColorActive);
        else if (dailySpendingLimit > 0) tvLimitLabel1.setTextColor(labelColorActive); // Healthy active if limit set & 0 spent
    }

    private void fetchTodaysTransactions(String authToken) {
        if (getContext() == null) return;
        isTransactionsLoading = true;
        updateLoadingIndicatorVisibility();
        if (rvTodaysTransactions != null) rvTodaysTransactions.setVisibility(View.GONE);
        if (tvNoTransactions != null) tvNoTransactions.setVisibility(View.GONE);

        RetrofitClient.getApiService().getTodaysTransactions("Bearer " + authToken).enqueue(new Callback<TodaysTransactionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<TodaysTransactionsResponse> call, @NonNull Response<TodaysTransactionsResponse> response) {
                isTransactionsLoading = false;
                updateLoadingIndicatorVisibility();
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null && response.body().getTransactions() != null) {
                    List<Transaction> fetchedTransactions = response.body().getTransactions();
                    Collections.sort(fetchedTransactions, Comparator.comparing(Transaction::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
                    transactionList.clear();
                    transactionList.addAll(fetchedTransactions);
                    transactionAdapter.notifyDataSetChanged();

                    if (transactionList.isEmpty()) {
                        tvNoTransactions.setText("No transactions for today.");
                        tvNoTransactions.setVisibility(View.VISIBLE);
                        rvTodaysTransactions.setVisibility(View.GONE);
                    } else {
                        tvNoTransactions.setVisibility(View.GONE);
                        rvTodaysTransactions.setVisibility(View.VISIBLE);
                    }
                } else if (response.code() == 401) {
                    // Auth issues handled by other calls typically, no specific UI for just this
                } else {
                    String errorMsg = parseErrorBody(response);
                    Log.e(TAG, "Failed to fetch transactions. " + errorMsg);
                    Toast.makeText(getContext(), "Failed to load transactions: " + errorMsg, Toast.LENGTH_LONG).show();
                    tvNoTransactions.setText("Failed to load transactions. Tap to retry.");
                    tvNoTransactions.setOnClickListener(v -> fetchAllHomeData());
                    tvNoTransactions.setVisibility(View.VISIBLE);
                    rvTodaysTransactions.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TodaysTransactionsResponse> call, @NonNull Throwable t) {
                isTransactionsLoading = false;
                updateLoadingIndicatorVisibility();
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Fetch transactions failed: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error loading transactions. Check connection.", Toast.LENGTH_LONG).show();
                tvNoTransactions.setText("Network error. Tap to retry.");
                tvNoTransactions.setOnClickListener(v -> fetchAllHomeData());
                tvNoTransactions.setVisibility(View.VISIBLE);
                rvTodaysTransactions.setVisibility(View.GONE);
            }
        });
    }

    private void handleLogout() {
        if (getContext() == null) return;
        AuthTokenManager.clearAuthToken(getContext());
        setSummaryPlaceholders("N/A"); // Resets all UI elements including greeting and bars
        if (transactionList != null) transactionList.clear();
        if (transactionAdapter != null) transactionAdapter.notifyDataSetChanged();
        if (tvNoTransactions != null) {
            tvNoTransactions.setText("Please log in to view data.");
            tvNoTransactions.setVisibility(View.VISIBLE);
        }
        if (rvTodaysTransactions != null) rvTodaysTransactions.setVisibility(View.GONE);
        if (pbHomeLoading != null) pbHomeLoading.setVisibility(View.GONE);

        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    public void refreshAllData() {
        Log.d(TAG, "refreshAllData called.");
        if (isAdded() && getContext() != null) {
            fetchAllHomeData();
        }
    }

    public double getCurrentTotalBalance() {
        if (tvTotalBalanceValue == null || currencyFormatter == null) {
            Log.e(TAG, "getCurrentTotalBalance: tvTotalBalanceValue or currencyFormatter is null");
            return 0.0; // Or -1.0 to indicate an error state
        }
        String balanceText = tvTotalBalanceValue.getText().toString();
        try {
            Number balanceNumber = currencyFormatter.parse(balanceText);
            if (balanceNumber != null) {
                return balanceNumber.doubleValue();
            } else {
                Log.e(TAG, "getCurrentTotalBalance: Parsed number is null for text: " + balanceText);
                return 0.0; // Or -1.0
            }
        } catch (java.text.ParseException e) { // Explicitly qualify ParseException
            Log.e(TAG, "getCurrentTotalBalance: Could not parse balance text: " + balanceText, e);
            return 0.0; // Or -1.0, indicating parsing failed
        }
    }
}
