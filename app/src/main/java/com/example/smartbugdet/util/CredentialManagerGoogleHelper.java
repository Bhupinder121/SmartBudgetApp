package com.example.smartbugdet.util;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CredentialManagerGoogleHelper {

    private static final String TAG = "CredentialManagerHelper";

    // Callback interface for sign-in results
    public interface GoogleSignInCallback {
        void onSuccess(String idToken);
        void onError(String errorMessage);
    }

    // Basic client-side nonce generator (server-side generation is more secure)
    private static String generateNonce() {
        // A simple way to generate a nonce. For production, consider a more robust method
        // or server-generated nonces.
        byte[] nonceBytes = new byte[16];
        new SecureRandom().nextBytes(nonceBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : nonceBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
        // Or using UUID: return UUID.randomUUID().toString();
    }

    public static void signIn(Activity activity, String serverClientId, GoogleSignInCallback callback) {
        CredentialManager credentialManager = CredentialManager.create(activity.getApplicationContext());
        String nonce = generateNonce(); // Generate a nonce for security

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Set to true if you only want to show already-authorized accounts
                .setServerClientId(serverClientId)
                .setNonce(nonce) // Include the nonce in the request
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // It's recommended to use an Executor for the callback
        Executor executor = Executors.newSingleThreadExecutor(); // Or use ContextCompat.getMainExecutor(activity) for UI thread

        // CancellationSignal can be used to cancel the request if needed
        CancellationSignal cancellationSignal = new CancellationSignal();

        credentialManager.getCredentialAsync(
                activity, // Use activity context for UI prompts
                request,
                cancellationSignal,
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            // The credential will be a GoogleIdTokenCredential
                            // For Google Sign-In, it's typically a CustomCredential
                            CustomCredential credential = (CustomCredential) result.getCredential();
                            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                                String idToken = GoogleIdTokenCredential.createFrom(credential.getData())
                                        .getIdToken();
                                callback.onSuccess(idToken);
                            } else {
                                Log.e(TAG, "Unexpected credential type: " + credential.getType());
                                callback.onError("Unexpected credential type obtained.");
                            }
                        } catch (ClassCastException e) {
                            Log.e(TAG, "Credential is not a CustomCredential or not of expected type", e);
                            callback.onError("Unexpected credential format.");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "GetCredentialException: " + e.getClass().getName() + " : " + e.getMessage(), e);
                        // Handle different types of exceptions, e.g.,
                        // NoCredentialException, UserCancelledException, etc.
                        String userFriendlyMessage = "Google Sign-In failed: " + e.getMessage();
                        if (e instanceof androidx.credentials.exceptions.NoCredentialException) {
                            userFriendlyMessage = "No Google accounts found or user chose not to sign in.";
                        }
//                        else if (e instanceof androidx.credentials.exceptions.UserCancelledException) {
//                            userFriendlyMessage = "Google Sign-In was cancelled.";
//                        }
                        callback.onError(userFriendlyMessage);
                    }
                }
        );
    }
}
