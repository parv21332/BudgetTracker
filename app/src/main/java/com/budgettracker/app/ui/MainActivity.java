package com.budgettracker.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.ActivityMainBinding;
import com.budgettracker.app.utils.NotificationHelper;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    /** Launcher for POST_NOTIFICATIONS runtime permission (Android 13+). */
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> { /* user made their choice — channel already created */ });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create notification channel (safe to call every launch)
        NotificationHelper.createNotificationChannel(this);

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        setupNavigation();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.dashboardFragment,
                R.id.incomeFragment,
                R.id.expenseFragment,
                R.id.reportsFragment,
                R.id.settingsFragment
        ).build();

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        binding.bottomNavigation.setItemActiveIndicatorColor(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_light)));

        // Adjust fragment container bottom padding dynamically to sit above nav bar
        binding.bottomNavigation.post(() -> {
            int navHeight = binding.bottomNavigation.getHeight();
            binding.navHostFragment.setPadding(0, 0, 0, navHeight);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }
}
