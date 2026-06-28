package com.budgettracker.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.budgettracker.app.data.model.Expense;

import java.util.List;

/**
 * DAO for Expenses table operations.
 */
@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertExpense(Expense expense);

    @Update
    void updateExpense(Expense expense);

    @Delete
    void deleteExpense(Expense expense);

    @Query("DELETE FROM expenses WHERE id = :id AND user_id = :userId")
    void deleteById(int id, int userId);

    // All expenses for a user, newest first
    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses(int userId);

    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY date DESC")
    List<Expense> getAllExpensesSync(int userId);

    // Get expense by ID
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    Expense getExpenseById(int id);

    // Filter by category
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND category_name = :category ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByCategory(int userId, String category);

    // Filter by date range
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByDateRange(int userId, long startDate, long endDate);

    @Query("SELECT * FROM expenses WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<Expense> getExpensesByDateRangeSync(int userId, long startDate, long endDate);

    // Filter by category AND date range
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND category_name = :category AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByCategoryAndDate(int userId, String category, long startDate, long endDate);

    // Search
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND (category_name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY date DESC")
    LiveData<List<Expense>> searchExpenses(int userId, String query);

    // Total expense
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE user_id = :userId")
    double getTotalExpense(int userId);

    // Total expense in range
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate")
    double getTotalExpenseInRange(int userId, long startDate, long endDate);

    // Monthly expense
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE user_id = :userId AND strftime('%m', datetime(date/1000, 'unixepoch')) = :month AND strftime('%Y', datetime(date/1000, 'unixepoch')) = :year")
    double getMonthlyExpense(int userId, String month, String year);

    // Category-wise totals for pie chart
    @Query("SELECT category_name, COALESCE(SUM(amount), 0) AS total FROM expenses WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate GROUP BY category_name ORDER BY total DESC")
    List<CategoryTotal> getCategoryTotals(int userId, long startDate, long endDate);

    // Recent expenses (last 5)
    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY date DESC LIMIT 5")
    LiveData<List<Expense>> getRecentExpenses(int userId);

    // Count
    @Query("SELECT COUNT(*) FROM expenses WHERE user_id = :userId")
    int getExpenseCount(int userId);

    /**
     * POJO for category totals aggregation (used in pie chart).
     */
    class CategoryTotal {
        public String category_name;
        public double total;
    }
}
