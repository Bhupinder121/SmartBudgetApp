package com.example.smartbugdet.ui.usersetup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Using android.widget.Button for simplicity, cast if MaterialButton

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartbugdet.R;
import com.example.smartbugdet.UserSetupActivity;
import com.google.android.material.button.MaterialButton;

public class WelcomeScreenFragment extends Fragment {

    public WelcomeScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton getStartedButton = view.findViewById(R.id.btn_welcome_get_started);
        getStartedButton.setOnClickListener(v -> {
            if (getActivity() instanceof UserSetupActivity) {
                ((UserSetupActivity) getActivity()).navigateToStartingBalance();
            }
        });
    }
}