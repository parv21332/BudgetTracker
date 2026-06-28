package com.budgettracker.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.ExpenseDao;
import com.budgettracker.app.data.model.Expense;

import java.util.List;

/**
 * ExpenseRepository - single source of truth for Expense data.
 */
public class ExpenseRepository {

    private final ExpenseDao expenseDao;

    public ExpenseRepository(Application application) {
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
    }

    public void insert(Expense expense) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                expenseDao.insertExpense(expense));
    }

    public void update(Expense expense) {
        expense.setUpdatedAt(System.currentTimeMillis());
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                expenseDao.updateExpense(expense));
    }

    public void delete(int id, int userId) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                expenseDao.deleteById(id, userId));
    }

    public LiveData<List<Expense>> getAllExpenses(int userId) {
        return expenseDao.getAllExpenses(userId);
    }

    public List<Expense> getAllExpensesSync(int userId) {
        return expenseDao.getAllExpensesSync(userId);
    }

    public Expense getExpenseById(int id) {
        return expenseDao.getExpenseById(id);
    }

    public LiveData<List<Expense>> getExpensesByCategory(int userId, String category) {
        return expenseDao.getExpensesByCategory(userId, category);
    }

    public LiveData<List<Expense>> getExpensesByDateRange(int userId, long startDate, long endDate) {
        return expenseDao.getExpensesByDateRange(userId, startDate, endDate);
    }

    public List<Expense> getExpensesByDateRangeSync(int userId, long startDate, long endDate) {
        return expenseDao.getExpensesByDateRangeSync(userId, startDate, endDate);
    }

    public LiveData<List<Expense>> searchExpenses(int userId, String query) {
        return expenseDao.searchExpenses(userId, query);
    }

    public LiveData<List<Expense>> getRecentExpenses(int userId) {
        return expenseDao.getRecentExpenses(userId);
    }

    public double getTotalExpense(int userId) {
        return expenseDao.getTotalExpense(userId);
    }

    public double getTotalExpenseInRange(int userId, long startDate, long endDate) {
        return expenseDao.getTotalExpenseInRange(userId, startDate, endDate);
    }

    public double getMonthlyExpense(int userId, String month, String year) {
        return expenseDao.getMonthlyExpense(userId, month, year);
    }

    public List<ExpenseDao.CategoryTotal> getCategoryTotals(int userId, long startDate, long endDate) {
        return expenseDao.getCategoryTotals(userId, startDate, endDate);
    }
}
