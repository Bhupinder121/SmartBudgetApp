package com.example.smartbugdet.ui.usersetup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartbugdet.R;
import com.example.smartbugdet.UserSetupActivity;
import com.google.android.material.button.MaterialButton;

public class ConfirmationScreenFragment extends Fragment {

    public ConfirmationScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_confirmation_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton goToDashboardButton = view.findViewById(R.id.btn_confirmation_go_to_dashboard);
        goToDashboardButton.setOnClickListener(v -> {
            if (getActivity() instanceof UserSetupActivity) {
                ((UserSetupActivity) getActivity()).finishUserSetup();
            }
        });
    }
}