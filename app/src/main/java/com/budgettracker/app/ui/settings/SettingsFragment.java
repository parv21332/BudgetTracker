package com.budgettracker.app.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.FragmentSettingsBinding;
import com.budgettracker.app.ui.MainActivity;
import com.budgettracker.app.utils.BackupUtils;
import com.budgettracker.app.viewmodel.SettingsViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

/**
 * SettingsFragment - dark mode, backup, restore, change password, logout.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel settingsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupProfileSection();
        setupClickListeners();
        observeViewModel();
    }

    private void setupProfileSection() {
        binding.tvUserName.setText(settingsViewModel.getUserName());
        binding.tvUserEmail.setText(settingsViewModel.getUserEmail());
    }

    private void setupClickListeners() {
        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            settingsViewModel.toggleDarkMode(isChecked);
            MainActivity.applyDarkMode(isChecked);
        });

        // Change Name
        binding.btnChangeName.setOnClickListener(v -> showChangeNameDialog());

        // Change Password
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Backup database
        binding.btnBackup.setOnClickListener(v ->
                settingsViewModel.backupDatabase(requireActivity().getApplication()));

        // Restore database
        binding.btnRestore.setOnClickListener(v -> showRestoreDialog());

        // Logout
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showChangeNameDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_name, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_new_name);
        etName.setText(settingsViewModel.getUserName());

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Name")
                .setView(dialogView)
                .setPositiveButton("Update", (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    settingsViewModel.updateName(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);
        TextInputEditText etOld = dialogView.findViewById(R.id.et_old_password);
        TextInputEditText etNew = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.et_confirm_password);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (d, w) -> {
                    String oldPw = etOld.getText() != null ? etOld.getText().toString() : "";
                    String newPw = etNew.getText() != null ? etNew.getText().toString() : "";
                    String confirmPw = etConfirm.getText() != null ? etConfirm.getText().toString() : "";
                    settingsViewModel.changePassword(oldPw, newPw, confirmPw);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestoreDialog() {
        File[] backups = BackupUtils.getAvailableBackups();
        if (backups.length == 0) {
            Snackbar.make(requireView(), "No backups found in Downloads/BudgetTracker/",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        String[] names = new String[backups.length];
        for (int i = 0; i < backups.length; i++) names[i] = backups[i].getName();

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Backup to Restore")
                .setItems(names, (d, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Restore")
                            .setMessage("This will replace current data. The app will need to restart. Continue?")
                            .setPositiveButton("Restore", (d2, w) ->
                                    settingsViewModel.restoreDatabase(
                                            requireActivity().getApplication(),
                                            backups[which].getAbsolutePath()))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> {
                    settingsViewModel.logout();
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).logout();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void observeViewModel() {
        settingsViewModel.darkModeEnabled.observe(getViewLifecycleOwner(), enabled -> {
            binding.switchDarkMode.setChecked(enabled);
        });

        settingsViewModel.currentUserName.observe(getViewLifecycleOwner(), name -> {
            binding.tvUserName.setText(name);
        });

        settingsViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        settingsViewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.startsWith("SUCCESS:")) {
                Snackbar.make(requireView(), result.substring(8), Snackbar.LENGTH_LONG).show();
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
