package com.example.smartbugdet.ui.transactions;

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

import com.example.smartbugdet.R;
import com.example.smartbugdet.model.Transaction;
import com.example.smartbugdet.network.AllTransactionsResponse;
import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.util.AuthTokenManager;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionsFragment extends Fragment {

    private static final String TAG = "TransactionsFragment";

    private RecyclerView rvTransactions;
    private TransactionListAdapter transactionAdapter;
    private List<Transaction> allTransactions;
    private List<Transaction> filteredList;

    private ChipGroup chipGroupTransactionType;
    private ChipGroup chipGroupDateRange;

    private ProgressBar pbTransactionsLoading;
    private TextView tvNoTransactionsMessage;
    private TextView tvSpendingLabel, tvIncomeLabel;
    private TextView tvFilterRecentSummary, tvFilter1MonthSummary, tvFilter1YearSummary;

    private ApiService apiService;
    private SimpleDateFormat isoParser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = RetrofitClient.getApiService();
        allTransactions = new ArrayList<>();
        filteredList = new ArrayList<>();

        isoParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        isoParser.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Initialize all views
        rvTransactions = view.findViewById(R.id.rv_transactions);
        chipGroupTransactionType = view.findViewById(R.id.chip_group_transaction_type);
        chipGroupDateRange = view.findViewById(R.id.chip_group_date_range);
        pbTransactionsLoading = view.findViewById(R.id.pb_transactions_loading);
        tvNoTransactionsMessage = view.findViewById(R.id.tv_no_transactions_message);
        tvSpendingLabel = view.findViewById(R.id.tv_spending_label);
        tvIncomeLabel = view.findViewById(R.id.tv_income_label);
        tvFilterRecentSummary = view.findViewById(R.id.tv_filter_recent_summary);
        tvFilter1MonthSummary = view.findViewById(R.id.tv_filter_1month_summary);
        tvFilter1YearSummary = view.findViewById(R.id.tv_filter_1year_summary);

        // Setup RecyclerView with the new adapter
        transactionAdapter = new TransactionListAdapter(getContext(), filteredList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(transactionAdapter);

        // Setup Listeners
        setupChipListeners();
        setupSummaryCardListeners();

        fetchAllTransactions();
    }

    private void setupChipListeners() {
        chipGroupTransactionType.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
        chipGroupDateRange.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void setupSummaryCardListeners() {
        tvFilterRecentSummary.setOnClickListener(v -> chipGroupDateRange.check(R.id.chip_filter_7days));
        tvFilter1MonthSummary.setOnClickListener(v -> chipGroupDateRange.check(R.id.chip_filter_month));
        tvFilter1YearSummary.setOnClickListener(v -> chipGroupDateRange.check(R.id.chip_filter_year));
    }

    private void fetchAllTransactions() {
        if (getContext() == null) return;
        String authToken = AuthTokenManager.getAuthToken(getContext());
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(getContext(), "Please log in to view transactions.", Toast.LENGTH_SHORT).show();
            return;
        }

        pbTransactionsLoading.setVisibility(View.VISIBLE);
        rvTransactions.setVisibility(View.GONE);
        tvNoTransactionsMessage.setVisibility(View.GONE);

        apiService.getAllTransactions("Bearer " + authToken).enqueue(new Callback<AllTransactionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllTransactionsResponse> call, @NonNull Response<AllTransactionsResponse> response) {
                pbTransactionsLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getTransactions() != null) {
                    allTransactions.clear();
                    allTransactions.addAll(response.body().getTransactions());
                    Log.d(TAG, "Successfully fetched " + allTransactions.size() + " transactions.");
                    applyFilters();
                } else {
                    tvNoTransactionsMessage.setText("Failed to load transactions.");
                    tvNoTransactionsMessage.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Failed to fetch transactions. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllTransactionsResponse> call, @NonNull Throwable t) {
                pbTransactionsLoading.setVisibility(View.GONE);
                tvNoTransactionsMessage.setText("Network error. Please try again.");
                tvNoTransactionsMessage.setVisibility(View.VISIBLE);
                Log.e(TAG, "Network error fetching transactions", t);
            }
        });
    }

    private void applyFilters() {
        if (allTransactions == null) return;

        updateSummaryFilterUI();

        String typeFilter = "all";
        int selectedTypeId = chipGroupTransactionType.getCheckedChipId();
        if (selectedTypeId == R.id.chip_filter_income) typeFilter = "income";
        else if (selectedTypeId == R.id.chip_filter_expense) typeFilter = "expense";

        String rangeFilter = "all";
        int selectedDateId = chipGroupDateRange.getCheckedChipId();
        if (selectedDateId == R.id.chip_filter_7days) rangeFilter = "7days";
        else if (selectedDateId == R.id.chip_filter_month) rangeFilter = "month";
        else if (selectedDateId == R.id.chip_filter_year) rangeFilter = "year";

        final String finalRangeFilter = rangeFilter;
        List<Transaction> dateFilteredTransactions = allTransactions.stream()
                .filter(t -> isDateInRange(t.getDate(), finalRangeFilter))
                .collect(Collectors.toList());

        calculateAndDisplayRates(dateFilteredTransactions);

        final String finalTypeFilter = typeFilter;
        List<Transaction> finalList = dateFilteredTransactions.stream()
                .filter(t -> "all".equals(finalTypeFilter) || finalTypeFilter.equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());

        transactionAdapter.updateData(finalList);

        if (finalList.isEmpty()) {
            tvNoTransactionsMessage.setText("No transactions match your filters.");
            tvNoTransactionsMessage.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvNoTransactionsMessage.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
        }
    }

    private void calculateAndDisplayRates(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : transactions) {
            if ("income".equalsIgnoreCase(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        double total = totalIncome + totalExpense;
        int spendingPercentage = 0;
        int incomePercentage = 0;

        if (total > 0) {
            spendingPercentage = (int) ((totalExpense / total) * 100);
            incomePercentage = (int) ((totalIncome / total) * 100);
        }

        tvSpendingLabel.setText(String.format(Locale.getDefault(), "Spending @ %d%%", spendingPercentage));
        tvIncomeLabel.setText(String.format(Locale.getDefault(), "Income @ %d%%", incomePercentage));
    }

    private void updateSummaryFilterUI() {
        if (getContext() == null) return;
        int selectedDateId = chipGroupDateRange.getCheckedChipId();

        tvFilterRecentSummary.setBackgroundResource(R.drawable.rounded_transparent_bg_with_border);
        tvFilterRecentSummary.setTextColor(ContextCompat.getColor(getContext(), R.color.material_on_surface_emphasis_medium));
        tvFilter1MonthSummary.setBackgroundResource(R.drawable.rounded_transparent_bg_with_border);
        tvFilter1MonthSummary.setTextColor(ContextCompat.getColor(getContext(), R.color.material_on_surface_emphasis_medium));
        tvFilter1YearSummary.setBackgroundResource(R.drawable.rounded_transparent_bg_with_border);
        tvFilter1YearSummary.setTextColor(ContextCompat.getColor(getContext(), R.color.material_on_surface_emphasis_medium));

        TextView selectedView = null;
        if (selectedDateId == R.id.chip_filter_7days) {
            selectedView = tvFilterRecentSummary;
        } else if (selectedDateId == R.id.chip_filter_month) {
            selectedView = tvFilter1MonthSummary;
        } else if (selectedDateId == R.id.chip_filter_year) {
            selectedView = tvFilter1YearSummary;
        }

        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.rounded_yellow_button);
            selectedView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        }
    }

    private boolean isDateInRange(String dateString, String range) {
        if (dateString == null || dateString.isEmpty()) {
            return false;
        }
        try {
            Date transactionDate = isoParser.parse(dateString);

            Calendar endCal = Calendar.getInstance();
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59);
            Date endDate = endCal.getTime();

            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);

            switch (range) {
                case "7days":
                    startCal.add(Calendar.DAY_OF_YEAR, -6);
                    break;
                case "month":
                    startCal.add(Calendar.MONTH, -1);
                    break;
                case "year":
                    startCal.add(Calendar.YEAR, -1);
                    break;
                default:
                    return true;
            }
            Date startDate = startCal.getTime();
            return !transactionDate.before(startDate) && !transactionDate.after(endDate);

        } catch (ParseException e) {
            Log.e(TAG, "Could not parse date for filtering: " + dateString, e);
            return false;
        }
    }
}
