package com.budgettracker.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.data.repository.ExpenseRepository;
import com.budgettracker.app.data.repository.IncomeRepository;
import com.budgettracker.app.data.repository.UserRepository;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.utils.SessionManager;

import java.util.List;

/**
 * ViewModel for the Dashboard screen.
 * Provides total income, total expense, balance, recent transactions.
 */
public class DashboardViewModel extends AndroidViewModel {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final int userId;

    // LiveData for dashboard stats
    public final MutableLiveData<Double> totalIncome = new MutableLiveData<>(0.0);
    public final MutableLiveData<Double> totalExpense = new MutableLiveData<>(0.0);
    public final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);
    public final MutableLiveData<Double> monthlyIncome = new MutableLiveData<>(0.0);
    public final MutableLiveData<Double> monthlyExpense = new MutableLiveData<>(0.0);
    public final MutableLiveData<String> userName = new MutableLiveData<>("");

    // Recent transactions
    public LiveData<List<Income>> recentIncome;
    public LiveData<List<Expense>> recentExpenses;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        incomeRepository = new IncomeRepository(application);
        expenseRepository = new ExpenseRepository(application);
        userRepository = new UserRepository(application);
        sessionManager = new SessionManager(application);
        userId = sessionManager.getUserId();

        recentIncome = incomeRepository.getRecentIncome(userId);
        recentExpenses = expenseRepository.getRecentExpenses(userId);

        userName.setValue(sessionManager.getUserName());
        loadStats();
    }

    /**
     * Reload all stats from database (call after any transaction change).
     */
    public void loadStats() {
        new Thread(() -> {
            double income = incomeRepository.getTotalIncome(userId);
            double expense = expenseRepository.getTotalExpense(userId);
            totalIncome.postValue(income);
            totalExpense.postValue(expense);
            balance.postValue(income - expense);

            // Monthly stats
            int month = DateUtils.getCurrentMonth();
            int year = DateUtils.getCurrentYear();
            String monthStr = DateUtils.formatMonthForQuery(month);
            String yearStr = String.valueOf(year);

            double mIncome = incomeRepository.getMonthlyIncome(userId, monthStr, yearStr);
            double mExpense = expenseRepository.getMonthlyExpense(userId, monthStr, yearStr);
            monthlyIncome.postValue(mIncome);
            monthlyExpense.postValue(mExpense);
        }).start();
    }

    public int getUserId() {
        return userId;
    }
}
