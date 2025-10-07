package com.example.smartbugdet.ui.usersetup;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SpendingLimitFragment extends Fragment {

    private ChipGroup chipGroupQuickLimits;
    private Chip chip500, chip1000, chip1500;
    private TextInputLayout tilCustomLimit;
    private TextInputEditText etCustomLimit;
    private MaterialButton btnHelpMeDecide;
    private MaterialButton btnFinishSetup;

    private double selectedLimit = -1;

    public SpendingLimitFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_spending_limit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chipGroupQuickLimits = view.findViewById(R.id.chip_group_quick_limits);
        chip500 = view.findViewById(R.id.chip_limit_500);
        chip1000 = view.findViewById(R.id.chip_limit_1000);
        chip1500 = view.findViewById(R.id.chip_limit_1500);
        tilCustomLimit = view.findViewById(R.id.til_custom_limit);
        etCustomLimit = view.findViewById(R.id.et_custom_limit);
        btnHelpMeDecide = view.findViewById(R.id.btn_limit_help_me_decide);
        btnFinishSetup = view.findViewById(R.id.btn_limit_finish_setup);

        setupChipListeners();
        setupCustomLimitEditTextListener();

        btnHelpMeDecide.setOnClickListener(v -> {
            // Placeholder for "Help Me Decide" functionality
            Toast.makeText(getContext(), "Calculator feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnFinishSetup.setOnClickListener(v -> {
            validateAndProceed();
        });
    }

    private void setupChipListeners() {
        chipGroupQuickLimits.setOnCheckedChangeListener((group, checkedId) -> {
            selectedLimit = -1; // Reset
            tilCustomLimit.setError(null); // Clear custom error
            if (checkedId != View.NO_ID) {
                Chip selectedChip = group.findViewById(checkedId);
                if (selectedChip != null) {
                    String chipText = selectedChip.getText().toString().replace("₹", "");
                    try {
                        selectedLimit = Double.parseDouble(chipText);
                        // Clear custom input when a chip is selected
                        if (etCustomLimit.getText() != null && etCustomLimit.getText().length() > 0) {
                            etCustomLimit.setText("");
                        }
                    } catch (NumberFormatException e) {
                        selectedLimit = -1;
                    }
                }
            }
        });
    }

    private void setupCustomLimitEditTextListener() {
        etCustomLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Clear chip selection when custom text is entered
                    if (chipGroupQuickLimits.getCheckedChipId() != View.NO_ID) {
                        chipGroupQuickLimits.clearCheck();
                    }
                    tilCustomLimit.setError(null); // Clear error as user is typing
                    selectedLimit = -1; // Reset, will be parsed on finish
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateAndProceed() {
        String customLimitStr = etCustomLimit.getText().toString().trim();

        if (chipGroupQuickLimits.getCheckedChipId() != View.NO_ID) {
            // Chip is selected, selectedLimit should be set by listener
            if (selectedLimit < 0) { // Should not happen if listener works
                Toast.makeText(getContext(), "Invalid chip selection.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (!TextUtils.isEmpty(customLimitStr)) {
            // Custom input is provided
            try {
                selectedLimit = Double.parseDouble(customLimitStr);
                if (selectedLimit <= 0) {
                    tilCustomLimit.setError("Limit must be a positive amount.");
                    selectedLimit = -1; // Invalidate
                    return;
                }
                tilCustomLimit.setError(null);
            } catch (NumberFormatException e) {
                tilCustomLimit.setError("Invalid custom amount format.");
                selectedLimit = -1; // Invalidate
                return;
            }
        } else {
            // No chip selected and no custom input
            Toast.makeText(getContext(), "Please select or enter a spending limit.", Toast.LENGTH_LONG).show();
            selectedLimit = -1; // Ensure it's invalidated
            return;
        }

        // If we reach here, a valid limit should be in selectedLimit
        if (selectedLimit > 0) {
            if (getActivity() instanceof UserSetupActivity) {
                ((UserSetupActivity) getActivity()).navigateToConfirmation(selectedLimit);
            }
        } else {
            // Fallback, should be caught by earlier checks
            Toast.makeText(getContext(), "Please set a valid spending limit.", Toast.LENGTH_LONG).show();
        }
    }
}