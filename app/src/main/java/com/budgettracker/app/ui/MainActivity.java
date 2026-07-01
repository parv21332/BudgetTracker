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
import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.UserDao;
import com.budgettracker.app.databinding.ActivityMainBinding;
import com.budgettracker.app.ui.auth.AuthActivity;
import com.budgettracker.app.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode BEFORE setContentView
        applyDarkModeFromPrefs();

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
    }

    /**
     * Read dark mode preference from DB and apply AppCompatDelegate.
     * Called before setContentView so theme applies correctly.
     */
    private void applyDarkModeFromPrefs() {
        SessionManager session = new SessionManager(this);
        int userId = session.getUserId();
        if (userId > 0) {
            // Run synchronously on main thread before UI inflates
            try {
                UserDao userDao = BudgetDatabase.getDatabase(this).userDao();
                com.budgettracker.app.data.model.User user = userDao.getUserById(userId);
                if (user != null) {
                    AppCompatDelegate.setDefaultNightMode(
                            user.isDarkMode()
                                    ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO);
                }
            } catch (Exception e) {
                // Ignore — use system default
            }
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.dashboardFragment,
                R.id.incomeFragment,
                R.id.expenseFragment,
                R.id.reportsFragment,
                R.id.settingsFragment
        ).build();

        setSupportActionBar(binding.toolbar);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
    }

    public void logout() {
        new SessionManager(this).clearSession();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }
}
