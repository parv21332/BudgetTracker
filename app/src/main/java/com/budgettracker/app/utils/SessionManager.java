package com.budgettracker.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager - manages user login session using SharedPreferences.
 * Completely offline - no server tokens needed.
 */
public class SessionManager {

    private static final String PREF_NAME = "BudgetTrackerSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_REMEMBER_LOGIN = "rememberLogin";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Save login session after successful authentication.
     */
    public void saveSession(int userId, String name, String email, boolean rememberLogin) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_REMEMBER_LOGIN, rememberLogin);
        editor.apply();
    }

    /**
     * Check if user is logged in.
     * If "remember login" was not selected, session clears on app kill.
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public boolean isRememberLogin() {
        return prefs.getBoolean(KEY_REMEMBER_LOGIN, false);
    }

    /**
     * Clear session on logout.
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public void updateName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }
}
