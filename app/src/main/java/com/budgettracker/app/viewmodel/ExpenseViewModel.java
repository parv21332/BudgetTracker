package com.budgettracker.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.model.Category;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.data.repository.CategoryRepository;
import com.budgettracker.app.data.repository.ExpenseRepository;
import com.budgettracker.app.utils.SessionManager;

import java.util.List;

/**
 * ViewModel for Expense screens.
 */
public class ExpenseViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final int userId;

    public LiveData<List<Expense>> allExpenses;
    public LiveData<List<Category>> categories;
    public final MutableLiveData<String> operationResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        categoryRepository = new CategoryRepository(application);
        SessionManager session = new SessionManager(application);
        userId = session.getUserId();

        allExpenses = expenseRepository.getAllExpenses(userId);
        categories = categoryRepository.getAllCategories(userId);
    }

    public void addExpense(double amount, Integer categoryId, String categoryName,
                           long date, String notes) {
        if (amount <= 0) {
            operationResult.postValue("ERROR:Amount must be greater than 0");
            return;
        }
        if (categoryName == null || categoryName.trim().isEmpty()) {
            operationResult.postValue("ERROR:Category is required");
            return;
        }

        isLoading.postValue(true);
        new Thread(() -> {
            try {
                Expense expense = new Expense(userId, amount, categoryId,
                        categoryName.trim(), date, notes != null ? notes.trim() : "");
                expenseRepository.insert(expense);
                operationResult.postValue("SUCCESS:Expense added successfully");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void updateExpense(Expense expense) {
        if (expense.getAmount() <= 0) {
            operationResult.postValue("ERROR:Amount must be greater than 0");
            return;
        }
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                expenseRepository.update(expense);
                operationResult.postValue("SUCCESS:Expense updated successfully");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void deleteExpense(int expenseId) {
        new Thread(() -> expenseRepository.delete(expenseId, userId)).start();
        operationResult.postValue("SUCCESS:Expense deleted");
    }

    public LiveData<List<Expense>> searchExpenses(String query) {
        return expenseRepository.searchExpenses(userId, query);
    }

    public LiveData<List<Expense>> filterByCategory(String category) {
        return expenseRepository.getExpensesByCategory(userId, category);
    }

    public LiveData<List<Expense>> filterByDateRange(long start, long end) {
        return expenseRepository.getExpensesByDateRange(userId, start, end);
    }

    public Expense getExpenseById(int id) {
        return expenseRepository.getExpenseById(id);
    }

    public int getUserId() { return userId; }
}
