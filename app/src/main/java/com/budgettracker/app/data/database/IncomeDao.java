package com.budgettracker.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.budgettracker.app.data.model.Income;

import java.util.List;

/**
 * DAO for Income table operations.
 * All queries filtered by userId for multi-user support.
 */
@Dao
public interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertIncome(Income income);

    @Update
    void updateIncome(Income income);

    @Delete
    void deleteIncome(Income income);

    @Query("DELETE FROM income WHERE id = :id AND user_id = :userId")
    void deleteById(int id, int userId);

    // Fetch all income for a user, newest first
    @Query("SELECT * FROM income WHERE user_id = :userId ORDER BY date DESC")
    LiveData<List<Income>> getAllIncome(int userId);

    // Non-live version for export/report generation
    @Query("SELECT * FROM income WHERE user_id = :userId ORDER BY date DESC")
    List<Income> getAllIncomeSync(int userId);

    // Get income by ID
    @Query("SELECT * FROM income WHERE id = :id LIMIT 1")
    Income getIncomeById(int id);

    // Filter by date range
    @Query("SELECT * FROM income WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Income>> getIncomeByDateRange(int userId, long startDate, long endDate);

    @Query("SELECT * FROM income WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<Income> getIncomeByDateRangeSync(int userId, long startDate, long endDate);

    // Search by source
    @Query("SELECT * FROM income WHERE user_id = :userId AND (source LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY date DESC")
    LiveData<List<Income>> searchIncome(int userId, String query);

    // Total income for a user
    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE user_id = :userId")
    double getTotalIncome(int userId);

    // Total income in a date range (for monthly/weekly reports)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate")
    double getTotalIncomeInRange(int userId, long startDate, long endDate);

    // Monthly income totals
    @Query("SELECT COALESCE(SUM(amount), 0) FROM income WHERE user_id = :userId AND strftime('%m', datetime(date/1000, 'unixepoch')) = :month AND strftime('%Y', datetime(date/1000, 'unixepoch')) = :year")
    double getMonthlyIncome(int userId, String month, String year);

    // Recent income (last 5)
    @Query("SELECT * FROM income WHERE user_id = :userId ORDER BY date DESC LIMIT 5")
    LiveData<List<Income>> getRecentIncome(int userId);

    // Count
    @Query("SELECT COUNT(*) FROM income WHERE user_id = :userId")
    int getIncomeCount(int userId);
}
