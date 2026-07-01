package com.budgettracker.app.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.CategoryDao;
import com.budgettracker.app.data.database.ExpenseDao;
import com.budgettracker.app.data.model.Category;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.utils.SessionManager;

import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {

    private static final String TAG = "ExpenseViewModel";
    private final ExpenseDao expenseDao;
    private final CategoryDao categoryDao;
    private final int userId;

    public LiveData<List<Expense>> allExpenses;
    public LiveData<List<Category>> categories;
    public final MutableLiveData<String> operationResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        categoryDao = db.categoryDao();
        SessionManager session = new SessionManager(application);
        userId = session.getUserId();
        Log.d(TAG, "ExpenseViewModel init, userId=" + userId);

        allExpenses = expenseDao.getAllExpenses(userId);
        categories = categoryDao.getAllCategories(userId);
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
        if (userId <= 0) {
            operationResult.postValue("ERROR:Session expired. Please login again.");
            return;
        }

        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Expense expense = new Expense(userId, amount, categoryId,
                        categoryName.trim(), date, notes != null ? notes.trim() : "");
                long id = expenseDao.insertExpense(expense);
                Log.d(TAG, "Expense inserted with id=" + id);
                operationResult.postValue("SUCCESS:Expense added successfully");
            } catch (Exception e) {
                Log.e(TAG, "Insert expense error: " + e.getMessage(), e);
                operationResult.postValue("ERROR:Failed to save: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void updateExpense(Expense expense) {
        isLoading.postValue(true);
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            try {
                expense.setUpdatedAt(System.currentTimeMillis());
                expenseDao.updateExpense(expense);
                operationResult.postValue("SUCCESS:Expense updated successfully");
            } catch (Exception e) {
                operationResult.postValue("ERROR:" + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void deleteExpense(int expenseId) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                expenseDao.deleteById(expenseId, userId));
    }

    public LiveData<List<Expense>> searchExpenses(String query) {
        return expenseDao.searchExpenses(userId, query);
    }

    public LiveData<List<Expense>> filterByCategory(String category) {
        return expenseDao.getExpensesByCategory(userId, category);
    }

    public LiveData<List<Expense>> filterByDateRange(long start, long end) {
        return expenseDao.getExpensesByDateRange(userId, start, end);
    }

    public Expense getExpenseById(int id) {
        return expenseDao.getExpenseById(id);
    }

    public int getUserId() { return userId; }
}
