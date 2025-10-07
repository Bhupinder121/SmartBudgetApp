package com.example.smartbugdet.ui.usersetup;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartbugdet.R;
import com.example.smartbugdet.UserSetupActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class StartingBalanceFragment extends Fragment {

    private TextInputLayout tilStartingBalance;
    private TextInputEditText etStartingBalance;

    public StartingBalanceFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_starting_balance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilStartingBalance = view.findViewById(R.id.til_starting_balance);
        etStartingBalance = view.findViewById(R.id.et_starting_balance);
        MaterialButton nextButton = view.findViewById(R.id.btn_balance_next);

        nextButton.setOnClickListener(v -> {
            String balanceStr = etStartingBalance.getText().toString().trim();
            if (TextUtils.isEmpty(balanceStr)) {
                tilStartingBalance.setError("Please enter your current balance.");
                return;
            }

            try {
                double balance = Double.parseDouble(balanceStr);
                if (balance < 0) { // Balance can be 0, but not negative
                    tilStartingBalance.setError("Balance cannot be negative.");
                    return;
                }
                tilStartingBalance.setError(null); // Clear error

                if (getActivity() instanceof UserSetupActivity) {
                    ((UserSetupActivity) getActivity()).navigateToSpendingLimit(balance);
                }
            } catch (NumberFormatException e) {
                tilStartingBalance.setError("Invalid number format.");
            }
        });
    }
}