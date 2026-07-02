package com.budgettracker.app.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.utils.AppPrefs;
import com.budgettracker.app.utils.BackupUtils;

public class SettingsViewModel extends AndroidViewModel {

    private static final String TAG = "SettingsViewModel";

    public final MutableLiveData<String>  operationResult  = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading        = new MutableLiveData<>(false);
    public final MutableLiveData<Double>  budgetLimit      = new MutableLiveData<>(0.0);

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        // Load saved budget limit on init
        budgetLimit.setValue(AppPrefs.getBudgetLimit(application));
    }

    // ── Budget limit ─────────────────────────────────────────────────────────

    public double getBudgetLimit() {
        Double val = budgetLimit.getValue();
        return val != null ? val : 0.0;
    }

    public void saveBudgetLimit(double limit) {
        AppPrefs.setBudgetLimit(getApplication(), limit);
        budgetLimit.setValue(limit);
        operationResult.postValue("SUCCESS:Budget limit saved");
    }

    // ── Display name ──────────────────────────────────────────────────────────

    public String getDisplayName() {
        return AppPrefs.getDisplayName(getApplication());
    }

    public void saveDisplayName(String name) {
        AppPrefs.setDisplayName(getApplication(), name.trim());
        operationResult.postValue("SUCCESS:Name saved");
    }

    // ── Backup / Restore ─────────────────────────────────────────────────────

    public void backupDatabase(Context context) {
        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
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
}
