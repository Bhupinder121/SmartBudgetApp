package com.example.smartbugdet.ui.budgets;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.smartbugdet.R;
import com.example.smartbugdet.model.Transaction;
import com.example.smartbugdet.network.AllTransactionsResponse;
import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.HomeSummaryResponse;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.SpendingLimitRequest;
import com.example.smartbugdet.util.AuthTokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BudgetsFragment extends Fragment {

    private static final String TAG = "BudgetsFragment";

    private TextView tvCurrentDailyLimit, tvFinancialScoreValue, tvFinancialScoreFeedback;
    private TextInputLayout tilNewDailyLimit;
    private TextInputEditText etNewDailyLimit;
    private MaterialButton btnSaveDailyLimit;
    private ProgressBar pbBudgetsLoading;
    private ImageView ivScoreInfo;
    private ApiService apiService;

    private double dailySpendingLimit = 0.0;

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
        tvFinancialScoreValue = view.findViewById(R.id.tv_financial_score_value);
        tvFinancialScoreFeedback = view.findViewById(R.id.tv_financial_score_feedback);
        ivScoreInfo = view.findViewById(R.id.iv_score_info);

        btnSaveDailyLimit.setOnClickListener(v -> saveNewSpendingLimit());
        ivScoreInfo.setOnClickListener(v -> showScoreInfoDialog());

        fetchBudgetData();
    }

    private void fetchBudgetData() {
        if (getContext() == null) return;
        String authToken = AuthTokenManager.getAuthToken(getContext());
        if (authToken == null) {
            Toast.makeText(getContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        pbBudgetsLoading.setVisibility(View.VISIBLE);

        // First, fetch summary to get the spending limit
        apiService.getHomeSummaryData("Bearer " + authToken).enqueue(new Callback<List<HomeSummaryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<HomeSummaryResponse>> call, @NonNull Response<List<HomeSummaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    HomeSummaryResponse summary = response.body().get(0);
                    dailySpendingLimit = summary.getDailySpendingLimit();
                    tvCurrentDailyLimit.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(dailySpendingLimit));

                    // Now fetch transactions for score calculation
                    fetchTransactionsForScore(authToken);
                } else {
                    pbBudgetsLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load budget data.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HomeSummaryResponse>> call, @NonNull Throwable t) {
                pbBudgetsLoading.setVisibility(View.GONE);
                Log.e(TAG, "Failed to fetch summary data", t);
                Toast.makeText(getContext(), "Error loading budget data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTransactionsForScore(String authToken) {
        apiService.getAllTransactions("Bearer " + authToken).enqueue(new Callback<AllTransactionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllTransactionsResponse> call, @NonNull Response<AllTransactionsResponse> response) {
                pbBudgetsLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Transaction> transactions = response.body().getTransactions();
                    calculateFinancialScore(transactions);
                } else {
                    tvFinancialScoreFeedback.setText("Could not load transaction data for score.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllTransactionsResponse> call, @NonNull Throwable t) {
                pbBudgetsLoading.setVisibility(View.GONE);
                Log.e(TAG, "Failed to fetch transactions for score", t);
                tvFinancialScoreFeedback.setText("Error loading score data.");
            }
        });
    }

    private void calculateFinancialScore(List<Transaction> transactions) {
        int score = 500; // Starting baseline
        SimpleDateFormat isoParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        isoParser.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (dailySpendingLimit > 0 && transactions != null) {
            Map<Integer, Double> dailyExpenses = transactions.stream()
                    .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                    .collect(Collectors.groupingBy(
                            t -> {
                                Calendar cal = Calendar.getInstance();
                                try {
                                    Date date = isoParser.parse(t.getDate());
                                    cal.setTime(date);
                                } catch (ParseException e) {
                                    Log.e(TAG, "Could not parse date for score calculation", e);
                                }
                                return cal.get(Calendar.DAY_OF_YEAR);
                            },
                            Collectors.summingDouble(Transaction::getAmount)
                    ));

            long daysUnderLimit = dailyExpenses.values().stream().filter(dailyTotal -> dailyTotal <= dailySpendingLimit).count();
            long daysOverLimit = dailyExpenses.size() - daysUnderLimit;

            score += (daysUnderLimit * 10);
            score -= (daysOverLimit * 15);
        }

        long incomeTransactions = transactions.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .count();
        score += (incomeTransactions * 20);

        tvFinancialScoreValue.setText(String.valueOf(score));
        if (score > 600) {
            tvFinancialScoreFeedback.setText("Great job! You are managing your finances well.");
        } else if (score > 400) {
            tvFinancialScoreFeedback.setText("You are on the right track. Keep it up!");
        } else {
            tvFinancialScoreFeedback.setText("There is room for improvement. Stick to your budgets!");
        }
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

        tilNewDailyLimit.setError(null);
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
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Spending limit updated successfully!", Toast.LENGTH_SHORT).show();
                    etNewDailyLimit.setText("");
                    // Re-fetch all data to update UI and score
                    fetchBudgetData();
                } else {
                    pbBudgetsLoading.setVisibility(View.GONE);
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

    private void showScoreInfoDialog() {
        if (getContext() == null) return;

        String title = "How Your Score Is Calculated";
        String message = "Your score starts at 500 and changes based on your financial habits over the last 30 days:<br><br>"
                + "<b>Positive Actions (Add Points):</b><br>"
                + "• Stay under daily limit: +10 per day<br>"
                + "• Perfect week streak: +50 bonus<br>"
                + "• Contribute to savings: +25<br>"
                + "• Log new income: +20<br><br>"
                + "<b>Negative Actions (Subtract Points):</b><br>"
                + "• Exceed daily limit: -15 per day<br>"
                + "• No savings in a week: -10";

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Got it", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.active_nav_icon_tint));
    }
}
