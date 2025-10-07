package com.example.smartbugdet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

// Imports for EncryptedSharedPreferences and MasterKey are removed
// import androidx.security.crypto.EncryptedSharedPreferences;
// import androidx.security.crypto.MasterKey;

// Security-related exception imports are removed as they are not thrown by standard SharedPreferences
// import java.io.IOException;
// import java.security.GeneralSecurityException;

public class AuthTokenManager {

    private static final String TAG = "AuthTokenManager";
    // You might want to change the file name if you previously used an encrypted one
    // to avoid any potential conflicts or to clearly differentiate.
    // For this example, we'll keep it but be mindful if you have old encrypted data.
    private static final String PREF_FILE_NAME = "smartbudget_prefs"; // Changed for clarity
    private static final String PREF_KEY_AUTH_TOKEN = "auth_token";

    // Helper method to get standard SharedPreferences
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void saveAuthToken(Context context, String token) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Attempted to save a null or empty token.");
            return;
        }
        // No security exceptions to catch with standard SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_KEY_AUTH_TOKEN, token);
        editor.apply();
        Log.d(TAG, "Auth token saved (standard SharedPreferences).");
    }

    public static String getAuthToken(Context context) {
        // No security exceptions to catch
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.getString(PREF_KEY_AUTH_TOKEN, null);
    }

    public static void clearAuthToken(Context context) {
        // No security exceptions to catch
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_KEY_AUTH_TOKEN);
        editor.apply();
        Log.d(TAG, "Auth token cleared (standard SharedPreferences).");
    }
}
