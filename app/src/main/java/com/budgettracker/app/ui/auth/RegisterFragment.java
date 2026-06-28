package com.budgettracker.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.FragmentRegisterBinding;
import com.budgettracker.app.ui.MainActivity;
import com.budgettracker.app.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

/**
 * RegisterFragment - handles new user registration UI and logic.
 */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();
            String confirm = binding.etConfirmPassword.getText().toString();
            boolean remember = binding.cbRememberMe.isChecked();
            authViewModel.register(name, email, password, confirm, remember);
        });

        // Navigate back to Login
        binding.tvLogin.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_registerFragment_to_loginFragment));
    }

    private void observeViewModel() {
        authViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!loading);
        });

        authViewModel.authResult.observe(getViewLifecycleOwner(), result -> {
            if (result.success) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Snackbar.make(requireView(), result.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.error_red, null))
                        .show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
