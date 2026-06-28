package com.budgettracker.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.data.repository.IncomeRepository;
import com.budgettracker.app.utils.SessionManager;

import java.util.List;

/**
 * ViewModel for Income screens (list, add, edit).
 */
public class IncomeViewModel extends AndroidViewModel {

    private final IncomeRepository incomeRepository;
    private final int userId;

    public LiveData<List<Income>> allIncome;
    public final MutableLiveData<String> operationResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Current filter state
    private long filterStart = 0;
    private long filterEnd = Long.MAX_VALUE;
    private String searchQuery = "";

    public IncomeViewModel(@NonNull Application application) {
        super(application);
        incomeRepository = new IncomeRepository(application);
        SessionManager session = new SessionManager(application);
        userId = session.getUserId();
        allIncome = incomeRepository.getAllIncome(userId);
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

        isLoading.postValue(true);
        new Thread(() -> {
            try {
                Income income = new Income(userId, amount, source.trim(), date,
                        notes != null ? notes.trim() : "");
                incomeRepository.insert(income);
                operationResult.postValue("SUCCESS:Income added successfully");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void updateIncome(Income income) {
        if (income.getAmount() <= 0) {
            operationResult.postValue("ERROR:Amount must be greater than 0");
            return;
        }
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                incomeRepository.update(income);
                operationResult.postValue("SUCCESS:Income updated successfully");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void deleteIncome(int incomeId) {
        new Thread(() -> incomeRepository.delete(incomeId, userId)).start();
        operationResult.postValue("SUCCESS:Income deleted");
    }

    public LiveData<List<Income>> searchIncome(String query) {
        return incomeRepository.searchIncome(userId, query);
    }

    public LiveData<List<Income>> filterByDateRange(long start, long end) {
        return incomeRepository.getIncomeByDateRange(userId, start, end);
    }

    public Income getIncomeById(int id) {
        return incomeRepository.getIncomeById(id);
    }

    public int getUserId() { return userId; }
}
