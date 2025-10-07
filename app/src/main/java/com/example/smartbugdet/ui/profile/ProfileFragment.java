package com.example.smartbugdet.ui.profile;

import android.content.Intent; // Import Intent
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast; // Import Toast for feedback

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartbugdet.LoginActivity; // Import LoginActivity
import com.example.smartbugdet.R;
import com.example.smartbugdet.util.AuthTokenManager; // Import AuthTokenManager

public class ProfileFragment extends Fragment {

    private Button btnSignOut; // Already declared

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSignOut = view.findViewById(R.id.sign_out); // Use the ID from the XML

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                // Clear the authentication token
                AuthTokenManager.clearAuthToken(requireContext());

                // Give some feedback to the user
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();

                // Navigate to LoginActivity
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                // Clear the back stack and start LoginActivity as a new task
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // Finish the current activity (MainActivity)
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }
    }
}
