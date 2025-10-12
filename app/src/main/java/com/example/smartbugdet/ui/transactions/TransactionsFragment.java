package com.example.smartbugdet.ui.transactions;

import android.graphics.Color;
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
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private LineChart lineChart;
    private ChipGroup chipGroupTransactionType;
    private ChipGroup chipGroupDateRange;
    private ChipGroup chipGroupGraphFilter;
    private Chip chipGraphIncome, chipGraphExpense;

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
        lineChart = view.findViewById(R.id.line_chart_transactions);
        chipGroupTransactionType = view.findViewById(R.id.chip_group_transaction_type);
        chipGroupDateRange = view.findViewById(R.id.chip_group_date_range);
        chipGroupGraphFilter = view.findViewById(R.id.chip_group_graph_filter);
        chipGraphIncome = view.findViewById(R.id.chip_graph_income);
        chipGraphExpense = view.findViewById(R.id.chip_graph_expense);
        pbTransactionsLoading = view.findViewById(R.id.pb_transactions_loading);
        tvNoTransactionsMessage = view.findViewById(R.id.tv_no_transactions_message);
        tvSpendingLabel = view.findViewById(R.id.tv_spending_label);
        tvIncomeLabel = view.findViewById(R.id.tv_income_label);
        tvFilterRecentSummary = view.findViewById(R.id.tv_filter_recent_summary);
        tvFilter1MonthSummary = view.findViewById(R.id.tv_filter_1month_summary);
        tvFilter1YearSummary = view.findViewById(R.id.tv_filter_1year_summary);

        // Setup RecyclerView
        transactionAdapter = new TransactionListAdapter(getContext(), filteredList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(transactionAdapter);

        // Setup Listeners
        setupChipListeners();
        setupSummaryCardListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAllTransactions();
    }

    private void setupChipListeners() {
        chipGroupTransactionType.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
        chipGroupDateRange.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
        
        View.OnClickListener graphChipClickListener = v -> applyFilters();
        chipGraphIncome.setOnClickListener(graphChipClickListener);
        chipGraphExpense.setOnClickListener(graphChipClickListener);
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
        lineChart.setVisibility(View.GONE);
        tvNoTransactionsMessage.setVisibility(View.GONE);

        apiService.getAllTransactions("Bearer " + authToken).enqueue(new Callback<AllTransactionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllTransactionsResponse> call, @NonNull Response<AllTransactionsResponse> response) {
                pbTransactionsLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getTransactions() != null) {
                    allTransactions.clear();
                    allTransactions.addAll(response.body().getTransactions());

                    // Sort all transactions by date in descending order immediately after fetching
                    Collections.sort(allTransactions, Comparator.comparing(Transaction::getDate).reversed());

                    Log.d(TAG, "Successfully fetched and sorted " + allTransactions.size() + " transactions.");
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
        setupLineChart(dateFilteredTransactions);

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

    private void setupLineChart(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty() || getContext() == null) {
            lineChart.setVisibility(View.GONE);
            return;
        }
        lineChart.setVisibility(View.VISIBLE);

        SimpleDateFormat dayFormatter = new SimpleDateFormat("MMM dd", Locale.getDefault());
        Map<String, float[]> dailyTotals = new LinkedHashMap<>();

        for (Transaction t : transactions) {
            try {
                Date date = isoParser.parse(t.getDate());
                String dayKey = dayFormatter.format(date);
                dailyTotals.putIfAbsent(dayKey, new float[]{0, 0}); // 0 for income, 1 for expense
                float[] amounts = dailyTotals.get(dayKey);
                if ("income".equalsIgnoreCase(t.getType())) {
                    amounts[0] += (float) t.getAmount();
                } else {
                    amounts[1] += (float) t.getAmount();
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date for chart: " + t.getDate(), e);
            }
        }

        List<String> labels = new ArrayList<>(dailyTotals.keySet());
        
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        int i = 0;
        for (String dayKey : labels) {
            float[] totals = dailyTotals.get(dayKey);
            if (totals != null) {
                incomeEntries.add(new Entry(i, totals[0]));
                expenseEntries.add(new Entry(i, totals[1]));
                i++;
            }
        }

        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Income");
        incomeDataSet.setColor(ContextCompat.getColor(getContext(), R.color.income_green));
        incomeDataSet.setCircleColor(ContextCompat.getColor(getContext(), R.color.income_green));
        incomeDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        incomeDataSet.setDrawFilled(true);
        incomeDataSet.setFillColor(ContextCompat.getColor(getContext(), R.color.income_green));
        incomeDataSet.setFillAlpha(100);

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Expense");
        expenseDataSet.setColor(ContextCompat.getColor(getContext(), R.color.expense_red));
        expenseDataSet.setCircleColor(ContextCompat.getColor(getContext(), R.color.expense_red));
        expenseDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        expenseDataSet.setDrawFilled(true);
        expenseDataSet.setFillColor(ContextCompat.getColor(getContext(), R.color.expense_red));
        expenseDataSet.setFillAlpha(100);
        
        incomeDataSet.setVisible(chipGraphIncome.isChecked());
        expenseDataSet.setVisible(chipGraphExpense.isChecked());

        LineData lineData = new LineData(incomeDataSet, expenseDataSet);
        lineData.setValueTextColor(Color.WHITE);
        lineData.setValueTextSize(10f);

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisRight().setEnabled(false);

        lineChart.setData(lineData);
        lineChart.animateY(1000, Easing.EaseInOutCubic); // Smoother vertical animation
        lineChart.invalidate();
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
