package com.budgettracker.app.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.ActivityAuthBinding;

/**
 * AuthActivity - container for Login and Register fragments.
 * Uses Navigation Component to switch between Login and Register.
 */
public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
