package com.budgettracker.app.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.IncomeDao;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.utils.AppPrefs;

import java.util.List;

public class IncomeViewModel extends AndroidViewModel {

    private static final String TAG = "IncomeViewModel";
    private final IncomeDao incomeDao;
    private final int userId;

    public LiveData<List<Income>> allIncome;
    public final MutableLiveData<String> operationResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public IncomeViewModel(@NonNull Application application) {
        super(application);
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        incomeDao = db.incomeDao();
        userId = AppPrefs.USER_ID;
        Log.d(TAG, "IncomeViewModel init, userId=" + userId);
        allIncome = incomeDao.getAllIncome(userId);
    }

    public void addIncome(double amount, String source, long date, String notes) {
        if (amount <= 0) {
            operationResult.postValue("ERROR:Amount must be greater than 0");
            return;
        }
        if (source == null || source.trim().isEmpty()) {
            operationResult.postValue("ERROR:Source is required");
            return;
        }
        if (userId <= 0) {
            operationResult.postValue("ERROR:Could not identify user");
            return;
        }

        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Income income = new Income(userId, amount, source.trim(), date,
                        notes != null ? notes.trim() : "");
                long id = incomeDao.insertIncome(income);
                Log.d(TAG, "Income inserted with id=" + id);
                operationResult.postValue("SUCCESS:Income added successfully");
            } catch (Exception e) {
                Log.e(TAG, "Insert income error: " + e.getMessage(), e);
                operationResult.postValue("ERROR:Failed to save: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void updateIncome(Income income) {
        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                income.setUpdatedAt(System.currentTimeMillis());
                incomeDao.updateIncome(income);
                operationResult.postValue("SUCCESS:Income updated successfully");
            } catch (Exception e) {
                operationResult.postValue("ERROR:" + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void deleteIncome(int incomeId) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                incomeDao.deleteById(incomeId, userId));
    }

    public LiveData<List<Income>> searchIncome(String query) {
        return incomeDao.searchIncome(userId, query);
    }

    public LiveData<List<Income>> filterByDateRange(long start, long end) {
        return incomeDao.getIncomeByDateRange(userId, start, end);
    }

    public Income getIncomeById(int id) {
        return incomeDao.getIncomeById(id);
    }

    public int getUserId() { return userId; }
}
