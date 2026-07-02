package com.budgettracker.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.FragmentSettingsBinding;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.viewmodel.SettingsViewModel;
import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Pre-fill display name
        String savedName = settingsViewModel.getDisplayName();
        if (!savedName.equals("User")) {
            binding.etDisplayName.setText(savedName);
        }

        // Pre-fill budget limit field
        double current = settingsViewModel.getBudgetLimit();
        if (current > 0) {
            binding.etBudgetLimit.setText(CurrencyUtils.formatPlain(current));
        }

        binding.btnSaveName.setOnClickListener(v -> saveDisplayName());
        binding.btnSaveBudget.setOnClickListener(v -> saveBudgetLimit());

        observeViewModel();
    }

    private void saveDisplayName() {
        String name = binding.etDisplayName.getText() != null
                ? binding.etDisplayName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            binding.tilDisplayName.setError("Name cannot be empty");
            return;
        }
        binding.tilDisplayName.setError(null);
        settingsViewModel.saveDisplayName(name);
    }

    private void saveBudgetLimit() {
        String raw = binding.etBudgetLimit.getText() != null
                ? binding.etBudgetLimit.getText().toString().trim() : "";

        if (raw.isEmpty()) {
            binding.tilBudgetLimit.setError("Enter a budget amount");
            return;
        }
        binding.tilBudgetLimit.setError(null);

        try {
            double limit = Double.parseDouble(raw);
            if (limit <= 0) {
                binding.tilBudgetLimit.setError("Amount must be greater than 0");
                return;
            }
            settingsViewModel.saveBudgetLimit(limit);
        } catch (NumberFormatException e) {
            binding.tilBudgetLimit.setError("Invalid number");
        }
    }

    private void observeViewModel() {
        settingsViewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null || binding == null || !isAdded()) return;
            settingsViewModel.operationResult.postValue(null);

            if (result.startsWith("SUCCESS:")) {
                String msg = result.substring(8);
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
            } else if (result.startsWith("ERROR:")) {
                Snackbar.make(requireView(), result.substring(6), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(requireContext().getColor(R.color.error_red))
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
