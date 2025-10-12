package com.example.smartbugdet;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ML extends AppCompatActivity {
    private static final String TAG = "ML_Activity";

    private FloatingActionButton butt;
    private LlmInference llmInference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        butt = findViewById(R.id.addTransactionFab);
        butt.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(ML.this, "Analyzing SMS inbox...", Toast.LENGTH_SHORT).show();
                analyzeSmsMessages();
            } else {
                Toast.makeText(this, "SMS permission not granted. Please grant it from settings.", Toast.LENGTH_LONG).show();
            }
        });

        initializeLlm();
    }

    private void initializeLlm() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                LlmInference.LlmInferenceOptions taskOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath("/data/local/tmp/llm/q8.task")
                        .build();
                llmInference = LlmInference.createFromOptions(getApplicationContext(), taskOptions);
                Log.d(TAG, "LlmInference initialized successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize LlmInference", e);
                runOnUiThread(() -> Toast.makeText(ML.this, "Error initializing AI model.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void analyzeSmsMessages() {
        if (llmInference == null) {
            Toast.makeText(this, "AI Model not ready. Please try again.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "LlmInference is not initialized yet. Attempting to reinitialize.");
            initializeLlm();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<String> smsMessages = new ArrayList<>();
            try (Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"body"}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int bodyColumnIndex = cursor.getColumnIndexOrThrow("body");
                    do {
                        smsMessages.add(cursor.getString(bodyColumnIndex));
                    } while (cursor.moveToNext());
                }
            }

            if (smsMessages.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(ML.this, "No SMS messages found in inbox.", Toast.LENGTH_SHORT).show());
                return;
            }

            String promptTemplate = "Analyze this SMS message. If it reports a debit or a payment sent from an account, respond with only the monetary amount. Otherwise, respond with only the word NULL. The message is: %s";
            int transactionCount = 0;
            for (String message : smsMessages) {
                try {
                    String result = llmInference.generateResponse(String.format(promptTemplate, message));
                    if (!result.trim().equalsIgnoreCase("NULL")) {
                        transactionCount++;
                        Log.i(TAG, "Transaction found: \"" + message + "\" -> Amount: " + result);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error generating response for message: " + message, e);
                }
            }

            int finalTransactionCount = transactionCount;
            runOnUiThread(() -> {
                String toastMessage = "Analysis complete. Found " + finalTransactionCount + " potential transactions. See logs.";
                Toast.makeText(ML.this, toastMessage, Toast.LENGTH_LONG).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (llmInference != null) {
            llmInference.close();
        }
    }
}
