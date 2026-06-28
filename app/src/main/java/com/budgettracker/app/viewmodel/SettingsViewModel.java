package com.budgettracker.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.repository.UserRepository;
import com.budgettracker.app.utils.BackupUtils;
import com.budgettracker.app.utils.SessionManager;

/**
 * ViewModel for Settings screen.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public final MutableLiveData<String> operationResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> darkModeEnabled = new MutableLiveData<>(false);
    public final MutableLiveData<String> currentUserName = new MutableLiveData<>("");

    private final int userId;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getUserId();
        currentUserName.setValue(sessionManager.getUserName());

        // Load current dark mode setting
        new Thread(() -> {
            com.budgettracker.app.data.model.User user = userRepository.getCurrentUser();
            if (user != null) {
                darkModeEnabled.postValue(user.isDarkMode());
            }
        }).start();
    }

    public void changePassword(String oldPassword, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            operationResult.postValue("ERROR:New password must be at least 6 characters");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            operationResult.postValue("ERROR:Passwords do not match");
            return;
        }

        isLoading.postValue(true);
        new Thread(() -> {
            try {
                boolean success = userRepository.changePassword(userId, oldPassword, newPassword);
                if (success) {
                    operationResult.postValue("SUCCESS:Password changed successfully");
                } else {
                    operationResult.postValue("ERROR:Current password is incorrect");
                }
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void toggleDarkMode(boolean enabled) {
        darkModeEnabled.setValue(enabled);
        userRepository.updateDarkMode(userId, enabled);
    }

    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            operationResult.postValue("ERROR:Name cannot be empty");
            return;
        }
        userRepository.updateName(userId, name.trim());
        currentUserName.setValue(name.trim());
        operationResult.postValue("SUCCESS:Name updated");
    }

    public void backupDatabase(Application application) {
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                String path = BackupUtils.backupDatabase(application);
                if (path != null) {
                    operationResult.postValue("SUCCESS:Backup saved to: " + path);
                } else {
                    operationResult.postValue("ERROR:Backup failed");
                }
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void restoreDatabase(Application application, String filePath) {
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                boolean success = BackupUtils.restoreDatabase(application, filePath);
                if (success) {
                    operationResult.postValue("SUCCESS:Database restored. Please restart the app.");
                } else {
                    operationResult.postValue("ERROR:Restore failed. File not found.");
                }
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void logout() {
        userRepository.logout();
    }

    public int getUserId() { return userId; }
    public String getUserName() { return sessionManager.getUserName(); }
    public String getUserEmail() { return sessionManager.getUserEmail(); }
}
