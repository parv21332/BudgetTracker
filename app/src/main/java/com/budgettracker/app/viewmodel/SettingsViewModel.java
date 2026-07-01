package com.budgettracker.app.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.UserDao;
import com.budgettracker.app.utils.BackupUtils;
import com.budgettracker.app.utils.PasswordUtils;
import com.budgettracker.app.utils.SessionManager;

public class SettingsViewModel extends AndroidViewModel {

    private static final String TAG = "SettingsViewModel";
    private final UserDao userDao;
    private final SessionManager sessionManager;

    public final MutableLiveData<String> operationResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> currentUserName = new MutableLiveData<>("");

    private final int userId;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        userDao = db.userDao();
        sessionManager = new SessionManager(application);
        userId = sessionManager.getUserId();
        currentUserName.setValue(sessionManager.getUserName());
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
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                com.budgettracker.app.data.model.User user = userDao.getUserById(userId);
                if (user == null) {
                    operationResult.postValue("ERROR:User not found");
                    return;
                }
                String oldHash = PasswordUtils.hashPassword(oldPassword);
                if (!oldHash.equals(user.getPasswordHash())) {
                    operationResult.postValue("ERROR:Current password is incorrect");
                    return;
                }
                String newHash = PasswordUtils.hashPassword(newPassword);
                userDao.updatePassword(userId, newHash);
                operationResult.postValue("SUCCESS:Password changed successfully");
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            operationResult.postValue("ERROR:Name cannot be empty");
            return;
        }
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                userDao.updateName(userId, name.trim()));
        sessionManager.updateName(name.trim());
        currentUserName.setValue(name.trim());
        operationResult.postValue("SUCCESS:Name updated");
    }

    /** Backup DB to app's external files dir (no permission needed on Android 10+) */
    public void backupDatabase(Context context) {
        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Close WAL checkpoint before backup
                BudgetDatabase.getDatabase(context).getOpenHelper().getWritableDatabase()
                        .execSQL("PRAGMA wal_checkpoint(FULL)");

                String path = BackupUtils.backupDatabase(context);
                if (path != null) {
                    operationResult.postValue("SUCCESS:Backup saved:\n" + path);
                } else {
                    operationResult.postValue("ERROR:Backup failed. Check storage permission.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Backup error: " + e.getMessage(), e);
                operationResult.postValue("ERROR:Backup error: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void restoreDatabase(Context context, String filePath) {
        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                boolean success = BackupUtils.restoreDatabase(context, filePath);
                if (success) {
                    operationResult.postValue("SUCCESS:Restore complete. Please restart the app.");
                } else {
                    operationResult.postValue("ERROR:Restore failed. File may be invalid.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Restore error: " + e.getMessage(), e);
                operationResult.postValue("ERROR:Restore error: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void logout() {
        sessionManager.clearSession();
    }

    public int getUserId() { return userId; }
    public String getUserName() { return sessionManager.getUserName(); }
    public String getUserEmail() { return sessionManager.getUserEmail(); }
}
