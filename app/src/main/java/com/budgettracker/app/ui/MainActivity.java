package com.budgettracker.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.ActivityMainBinding;
import com.budgettracker.app.ui.auth.AuthActivity;
import com.budgettracker.app.utils.SessionManager;

/**
 * MainActivity - main container after login.
 * Hosts the Bottom Navigation and NavHostFragment.
 * Manages dark mode theming.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode before setContentView
        applyTheme();

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
    }

    private void applyTheme() {
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            // We'll load dark mode from DB, for now use session default
            // Dark mode is applied reactively from Settings
        }
    }

    public static void applyDarkMode(boolean enabled) {
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        // Define top-level destinations (no back arrow shown)
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.dashboardFragment,
                R.id.incomeFragment,
                R.id.expenseFragment,
                R.id.reportsFragment,
                R.id.settingsFragment
        ).build();

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Setup bottom navigation
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
    }

    /**
     * Called from SettingsFragment on logout.
     */
    public void logout() {
        new SessionManager(this).clearSession();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
