package com.budgettracker.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        setupProfileSection();
        setupClickListeners();
        observeViewModel();
    }

    private void setupProfileSection() {
        binding.tvUserName.setText(settingsViewModel.getUserName());
        binding.tvUserEmail.setText(settingsViewModel.getUserEmail());
    }

    private void setupClickListeners() {
        // Dark mode toggle — DO NOT trigger on initial observe
        binding.switchDarkMode.setOnCheckedChangeListener(null);

        // Change Name
        binding.btnChangeName.setOnClickListener(v -> showChangeNameDialog());

        // Change Password
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Backup database
        binding.btnBackup.setOnClickListener(v ->
                settingsViewModel.backupDatabase(requireContext()));

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
        List<File> backups = BackupUtils.getAvailableBackups(requireContext());

        if (backups.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("No Backups Found")
                    .setMessage("Backup location:\n" + BackupUtils.getBackupLocation(requireContext())
                            + "\n\nCreate a backup first using the 'Backup Data' button.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String[] names = new String[backups.size()];
        for (int i = 0; i < backups.size(); i++) {
            names[i] = backups.get(i).getName() + "\n("
                    + sdf.format(new Date(backups.get(i).lastModified())) + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Backup to Restore")
                .setItems(names, (d, which) -> {
                    File selected = backups.get(which);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Restore")
                            .setMessage("Restoring from:\n" + selected.getName()
                                    + "\n\nThis will replace all current data. App will restart. Continue?")
                            .setPositiveButton("Restore", (d2, w) ->
                                    settingsViewModel.restoreDatabase(
                                            requireContext(), selected.getAbsolutePath()))
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
        // Dark mode — set switch silently first, then attach listener
        settingsViewModel.darkModeEnabled.observe(getViewLifecycleOwner(), enabled -> {
            // Remove listener before setting value to avoid triggering toggleDarkMode
            binding.switchDarkMode.setOnCheckedChangeListener(null);
            binding.switchDarkMode.setChecked(enabled);
            // Re-attach listener after setting
            binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                    settingsViewModel.toggleDarkMode(isChecked));
        });

        // Recreate activity when dark mode changes
        settingsViewModel.recreateActivity.observe(getViewLifecycleOwner(), shouldRecreate -> {
            if (shouldRecreate != null && shouldRecreate && isAdded()) {
                settingsViewModel.recreateActivity.setValue(false);
                Boolean isDark = settingsViewModel.darkModeEnabled.getValue();
                // Apply theme
                AppCompatDelegate.setDefaultNightMode(
                        isDark != null && isDark
                                ? AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO);
                // Recreate to apply
                requireActivity().recreate();
            }
        });

        settingsViewModel.currentUserName.observe(getViewLifecycleOwner(), name -> {
            if (binding != null) binding.tvUserName.setText(name);
        });

        settingsViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                binding.btnBackup.setEnabled(!loading);
                binding.btnRestore.setEnabled(!loading);
            }
        });

        settingsViewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null || binding == null) return;
            if (result.startsWith("SUCCESS:")) {
                String msg = result.substring(8);
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();

                // If restore succeeded, restart app
                if (msg.contains("Restore complete") || msg.contains("restart")) {
                    new android.os.Handler().postDelayed(() -> {
                        if (isAdded()) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }, 2000);
                }
            } else if (result.startsWith("ERROR:")) {
                Snackbar.make(requireView(), result.substring(6), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(requireContext().getColor(R.color.error_red))
                        .show();
            }
            settingsViewModel.operationResult.setValue(null);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
