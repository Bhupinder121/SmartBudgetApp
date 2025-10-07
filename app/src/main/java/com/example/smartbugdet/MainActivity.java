package com.example.smartbugdet;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable; // Import for transparent background
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity; // Import for centering
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window; // Import for window manipulation
import android.view.WindowManager; // Import for window layout params
import android.widget.EditText;
import android.widget.ImageView;
// import android.widget.ProgressBar; // Already there
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartbugdet.network.ApiService;
import com.example.smartbugdet.network.GenericMessageResponse;
import com.example.smartbugdet.network.RetrofitClient;
import com.example.smartbugdet.network.TransactionRequest;
import com.example.smartbugdet.ui.budgets.BudgetsFragment;
import com.example.smartbugdet.ui.home.HomeFragment;
import com.example.smartbugdet.ui.profile.ProfileFragment;
import com.example.smartbugdet.ui.transactions.TransactionsFragment;
import com.example.smartbugdet.util.AuthTokenManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ImageView navHome;
    private ImageView navTransactions;
    private ImageView fabAddTransaction;
    private ImageView navBudgets;
    private ImageView navProfile;
    private ImageView currentSelectedNavView;

    // For Add Transaction Dialog
    private AlertDialog addTransactionDialog;
    private Calendar selectedDateCalendar;
    private String currentTransactionType = "expense"; // "expense" or "income"
    private String selectedDateString = ""; // YYYY-MM-DD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        navHome = findViewById(R.id.nav_home);
        navTransactions = findViewById(R.id.nav_transactions);
        fabAddTransaction = findViewById(R.id.fab_add_transaction);
        navBudgets = findViewById(R.id.nav_budgets);
        navProfile = findViewById(R.id.nav_profile);

        selectedDateCalendar = Calendar.getInstance(); // Initialize calendar

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), navHome);
        }

        navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), navHome));
        navTransactions.setOnClickListener(v -> loadFragment(new TransactionsFragment(), navTransactions));
        navBudgets.setOnClickListener(v -> loadFragment(new BudgetsFragment(), navBudgets));
        navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment(), navProfile));

        fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());
    }

    private void loadFragment(Fragment fragment, ImageView selectedNavView) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.nav_host_fragment_container, fragment);
        fragmentTransaction.commit();
        updateNavIconTints(selectedNavView);
    }

    private void updateNavIconTints(ImageView selectedNavView) {
        if (currentSelectedNavView != null && currentSelectedNavView != selectedNavView) {
            currentSelectedNavView.setColorFilter(ContextCompat.getColor(this, R.color.inactive_nav_icon_tint));
        }
        if (selectedNavView != null) { 
            selectedNavView.setColorFilter(ContextCompat.getColor(this, R.color.active_nav_icon_tint));
            currentSelectedNavView = selectedNavView;
        }
    }

    private void showAddTransactionDialog() {
        // Using Base_Theme_Smartbugdet as it likely defines text colors etc.
        // The window properties will be overridden programmatically.
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Base_Theme_Smartbugdet);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_transaction_dialog, null);
        builder.setView(dialogView);

        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        TextView tvDate = dialogView.findViewById(R.id.tv_date);
        TextView btnExpense = dialogView.findViewById(R.id.btn_expense);
        TextView btnIncome = dialogView.findViewById(R.id.btn_income);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnAdd = dialogView.findViewById(R.id.btn_add);

        updateDateInView(tvDate);
        currentTransactionType = "expense"; 
        updateTransactionTypeUI(btnExpense, btnIncome);

        btnExpense.setOnClickListener(v -> {
            currentTransactionType = "expense";
            updateTransactionTypeUI(btnExpense, btnIncome);
        });

        btnIncome.setOnClickListener(v -> {
            currentTransactionType = "income";
            updateTransactionTypeUI(btnExpense, btnIncome);
        });

        tvDate.setOnClickListener(v -> showDatePickerDialog(tvDate));

        addTransactionDialog = builder.create();

        // --- Start of Window Customization ---
        Window window = addTransactionDialog.getWindow();
        if (window != null) {
            // Make AlertDialog window transparent so CardView's background and corners are visible
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            
            // Set layout to match parent width, CardView margins will create spacing
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            
            // Set gravity to center
            window.setGravity(Gravity.CENTER);
            
            // Increase dimming of the background activity
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); // Ensure dimming is enabled
            window.setDimAmount(0.8f); // Adjust as desired (0.0f = no dim, 1.0f = full dim)
        }
        // --- End of Window Customization ---


        btnCancel.setOnClickListener(v -> addTransactionDialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String title = etTitle.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr) || amountStr.equals("₹") || amountStr.equals("₹ ")) {
                etAmount.setError("Amount is required");
                etAmount.requestFocus();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr.replace("₹", "").trim());
                if (amount <= 0) {
                    etAmount.setError("Amount must be greater than zero");
                    etAmount.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount format");
                etAmount.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title is required");
                etTitle.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(selectedDateString)) {
                Toast.makeText(MainActivity.this, "Please select a date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get current total balance from HomeFragment
            double balanceBeforeTransaction = 0.0; // Default value
            Fragment currentHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
            if (currentHostFragment instanceof HomeFragment) {
                balanceBeforeTransaction = ((HomeFragment) currentHostFragment).getCurrentTotalBalance();
                // HomeFragment's getCurrentTotalBalance returns 0.0 or -1.0 on error,
                // which is acceptable as a default if we can't get the real balance.
                Log.d(TAG, "Retrieved balanceBeforeTransaction from HomeFragment: " + balanceBeforeTransaction);
            } else {
                Log.w(TAG, "HomeFragment not found in nav_host_fragment_container. Defaulting balanceBeforeTransaction to 0.0.");
                // Potentially show a warning to the user or handle this case more gracefully
                // if sending an accurate 'pre_amount' is critical.
            }

            TransactionRequest transactionRequest = new TransactionRequest(
                title,
                amount,
                currentTransactionType,
                selectedDateString,
                balanceBeforeTransaction // New field added
            );
            sendTransactionToServer(transactionRequest);
        });

        addTransactionDialog.show();
    }

    private void updateTransactionTypeUI(TextView btnExpense, TextView btnIncome) {
        if ("expense".equals(currentTransactionType)) {
            btnExpense.setBackgroundResource(R.drawable.rounded_white_bg);
            btnExpense.setTextColor(ContextCompat.getColor(this, R.color.dialog_toggle_selected_text));
            btnIncome.setBackgroundResource(android.R.color.transparent); 
            btnIncome.setTextColor(ContextCompat.getColor(this, R.color.dialog_toggle_unselected_text));
        } else { 
            btnIncome.setBackgroundResource(R.drawable.rounded_white_bg);
            btnIncome.setTextColor(ContextCompat.getColor(this, R.color.dialog_toggle_selected_text));
            btnExpense.setBackgroundResource(android.R.color.transparent);
            btnExpense.setTextColor(ContextCompat.getColor(this, R.color.dialog_toggle_unselected_text));
        }
    }

    private void showDatePickerDialog(TextView tvDateDisplay) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateInView(tvDateDisplay);
        };

        // Using Base_Theme_Smartbugdet for DatePickerDialog as well, or your DatePickerTheme
        new DatePickerDialog(MainActivity.this, R.style.Base_Theme_Smartbugdet, dateSetListener,
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateDateInView(TextView tvDateDisplay) {
        SimpleDateFormat displaySdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        tvDateDisplay.setText(displaySdf.format(selectedDateCalendar.getTime()));

        SimpleDateFormat serverSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateString = serverSdf.format(selectedDateCalendar.getTime());
    }

    private void sendTransactionToServer(TransactionRequest transactionRequest) {
        String authToken = AuthTokenManager.getAuthToken(this);
        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            // Consider navigating to login or disabling UI that requires auth
            return;
        }

        // Show some loading indicator if you have one for the dialog
        // e.g., dialogProgressBar.setVisibility(View.VISIBLE);
        // btnAdd.setEnabled(false); // Disable button to prevent multiple clicks

        ApiService apiService = RetrofitClient.getApiService();
        Call<GenericMessageResponse> call = apiService.sendTransaction("Bearer " + authToken, transactionRequest);

        call.enqueue(new Callback<GenericMessageResponse>() {
            @Override
            public void onResponse(Call<GenericMessageResponse> call, Response<GenericMessageResponse> response) {
                // Hide loading indicator
                // e.g., dialogProgressBar.setVisibility(View.GONE);
                // btnAdd.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MainActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    if (addTransactionDialog != null && addTransactionDialog.isShowing()) {
                        addTransactionDialog.dismiss();
                    }

                    // Attempt to refresh HomeFragment if it's visible
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).refreshAllData(); // Corrected method call
                        Log.d(TAG, "HomeFragment found, refreshAllData() requested.");
                    } else {
                        Log.d(TAG, "HomeFragment not currently visible, no refresh sent.");
                    }

                } else if (response.code() == 401) { // Unauthorized
                    Toast.makeText(MainActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    AuthTokenManager.clearAuthToken(MainActivity.this);
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Close MainActivity
                } else {
                    // Try to get a more detailed error message from the error body
                    String errorMessage = "Failed to add transaction.";
                    if (response.errorBody() != null) {
                        try {
                            // It's important to not call response.errorBody().string() multiple times
                            String errorBodyStr = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBodyStr);
                            // You might want to parse this if it's JSON, e.g., using GenericMessageResponse or another model
                            // For now, just append it or part of it
                            errorMessage += " Server: " + errorBodyStr.substring(0, Math.min(errorBodyStr.length(), 100)); // Show a snippet
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error body for add transaction", e);
                            errorMessage += " Code: " + response.code();
                        }
                    } else {
                        errorMessage += " Code: " + response.code();
                    }
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericMessageResponse> call, Throwable t) {
                // Hide loading indicator
                // e.g., dialogProgressBar.setVisibility(View.GONE);
                // btnAdd.setEnabled(true);

                Log.e(TAG, "Send transaction request failed: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Network error. Could not add transaction. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

}
