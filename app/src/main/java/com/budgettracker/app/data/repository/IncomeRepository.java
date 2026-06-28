package com.budgettracker.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.IncomeDao;
import com.budgettracker.app.data.model.Income;

import java.util.List;

/**
 * IncomeRepository - single source of truth for Income data.
 * All write operations run on background thread via ExecutorService.
 */
public class IncomeRepository {

    private final IncomeDao incomeDao;

    public IncomeRepository(Application application) {
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        incomeDao = db.incomeDao();
    }

    public void insert(Income income) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                incomeDao.insertIncome(income));
    }

    public void update(Income income) {
        income.setUpdatedAt(System.currentTimeMillis());
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                incomeDao.updateIncome(income));
    }

    public void delete(int id, int userId) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                incomeDao.deleteById(id, userId));
    }

    public LiveData<List<Income>> getAllIncome(int userId) {
        return incomeDao.getAllIncome(userId);
    }

    public List<Income> getAllIncomeSync(int userId) {
        return incomeDao.getAllIncomeSync(userId);
    }

    public Income getIncomeById(int id) {
        return incomeDao.getIncomeById(id);
    }

    public LiveData<List<Income>> getIncomeByDateRange(int userId, long startDate, long endDate) {
        return incomeDao.getIncomeByDateRange(userId, startDate, endDate);
    }

    public List<Income> getIncomeByDateRangeSync(int userId, long startDate, long endDate) {
        return incomeDao.getIncomeByDateRangeSync(userId, startDate, endDate);
    }

    public LiveData<List<Income>> searchIncome(int userId, String query) {
        return incomeDao.searchIncome(userId, query);
    }

    public LiveData<List<Income>> getRecentIncome(int userId) {
        return incomeDao.getRecentIncome(userId);
    }

    public double getTotalIncome(int userId) {
        return incomeDao.getTotalIncome(userId);
    }

    public double getTotalIncomeInRange(int userId, long startDate, long endDate) {
        return incomeDao.getTotalIncomeInRange(userId, startDate, endDate);
    }

    public double getMonthlyIncome(int userId, String month, String year) {
        return incomeDao.getMonthlyIncome(userId, month, year);
    }
}
