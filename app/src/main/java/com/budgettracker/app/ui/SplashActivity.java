package com.budgettracker.app.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;

import com.budgettracker.app.R;
import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.model.User;
import com.budgettracker.app.ui.auth.AuthActivity;
import com.budgettracker.app.utils.SessionManager;

/**
 * SplashActivity - entry point of the app.
 * Uses Android 12 SplashScreen API.
 * Redirects to Login or Dashboard based on session state.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved dark mode preference BEFORE splash screen installs
        applyDarkModePreference();

        // Install splash screen (Android 12+ native API)
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Keep splash visible until navigation is ready
        splashScreen.setKeepOnScreenCondition(() -> false);

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY);
    }

    private void navigateNext() {
        SessionManager session = new SessionManager(this);
        Intent intent;

        if (session.isLoggedIn()) {
            // User already logged in → go to main screen
            intent = new Intent(this, MainActivity.class);
        } else {
            // Not logged in → go to auth screen
            intent = new Intent(this, AuthActivity.class);
        }

        startActivity(intent);
        finish(); // Remove splash from back stack
    }

    /**
     * Read the saved dark mode setting for the logged-in user and apply it
     * via AppCompatDelegate so it takes effect for the entire app session.
     * This runs synchronously on the main thread — it's fast (single DB read).
     */
    private void applyDarkModePreference() {
        SessionManager session = new SessionManager(this);
        int userId = session.getUserId();
        if (userId == -1) {
            // Not logged in yet — use system default
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            return;
        }
        try {
            User user = BudgetDatabase.getDatabase(this).userDao().getUserById(userId);
            if (user != null && user.isDarkMode()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } catch (Exception e) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
