package com.budgettracker.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.database.ExpenseDao;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.data.model.MonthlyReport;
import com.budgettracker.app.data.repository.ExpenseRepository;
import com.budgettracker.app.data.repository.IncomeRepository;
import com.budgettracker.app.utils.AppPrefs;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.utils.ExportUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * ViewModel for Reports screen.
 * Loads monthly/weekly report data and handles export.
 */
public class ReportsViewModel extends AndroidViewModel {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final int userId;
    private final Application app;

    public final MutableLiveData<MonthlyReport> currentReport = new MutableLiveData<>();
    public final MutableLiveData<List<MonthlyReport>> monthlyHistory = new MutableLiveData<>();
    public final MutableLiveData<List<ExpenseDao.CategoryTotal>> categoryTotals = new MutableLiveData<>();
    public final MutableLiveData<String> exportResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Selected month/year for report
    private int selectedMonth = DateUtils.getCurrentMonth();
    private int selectedYear = DateUtils.getCurrentYear();

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        this.app = application;
        incomeRepository = new IncomeRepository(application);
        expenseRepository = new ExpenseRepository(application);
        userId = AppPrefs.USER_ID;
        loadCurrentMonthReport();
    }

    public void loadCurrentMonthReport() {
        loadReport(selectedMonth, selectedYear);
    }

    public void loadReport(int month, int year) {
        this.selectedMonth = month;
        this.selectedYear = year;
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                long startDate = DateUtils.getStartOfMonth(month, year);
                long endDate = DateUtils.getEndOfMonth(month, year);

                double income = incomeRepository.getTotalIncomeInRange(userId, startDate, endDate);
                double expense = expenseRepository.getTotalExpenseInRange(userId, startDate, endDate);
                List<Income> incomeList = incomeRepository.getIncomeByDateRangeSync(userId, startDate, endDate);
                List<Expense> expenseList = expenseRepository.getExpensesByDateRangeSync(userId, startDate, endDate);

                MonthlyReport report = new MonthlyReport(month, year, income, expense,
                        incomeList.size() + expenseList.size());
                currentReport.postValue(report);

                // Category totals for pie chart
                List<ExpenseDao.CategoryTotal> cats = expenseRepository.getCategoryTotals(userId, startDate, endDate);
                categoryTotals.postValue(cats);

                // Load last 6 months history
                loadMonthlyHistory();
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void loadWeeklyReport() {
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                long startDate = DateUtils.getStartOfCurrentWeek();
                long endDate = DateUtils.getEndOfCurrentWeek();
                double income = incomeRepository.getTotalIncomeInRange(userId, startDate, endDate);
                double expense = expenseRepository.getTotalExpenseInRange(userId, startDate, endDate);
                MonthlyReport report = new MonthlyReport(
                        DateUtils.getCurrentMonth(), DateUtils.getCurrentYear(),
                        income, expense, 0);
                currentReport.postValue(report);

                List<ExpenseDao.CategoryTotal> cats = expenseRepository.getCategoryTotals(userId, startDate, endDate);
                categoryTotals.postValue(cats);
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    private void loadMonthlyHistory() {
        List<MonthlyReport> history = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 6; i++) {
            int m = cal.get(Calendar.MONTH) + 1;
            int y = cal.get(Calendar.YEAR);
            long s = DateUtils.getStartOfMonth(m, y);
            long e = DateUtils.getEndOfMonth(m, y);
            double inc = incomeRepository.getTotalIncomeInRange(userId, s, e);
            double exp = expenseRepository.getTotalExpenseInRange(userId, s, e);
            history.add(new MonthlyReport(m, y, inc, exp, 0));
            cal.add(Calendar.MONTH, -1);
        }
        monthlyHistory.postValue(history);
    }

    public void exportToPdf(String currencySymbol) {
        isLoading.postValue(true);
        new Thread(() -> {
            try {
                long startDate = DateUtils.getStartOfMonth(selectedMonth, selectedYear);
                long endDate = DateUtils.getEndOfMonth(selectedMonth, selectedYear);
                List<Income> incomeList = incomeRepository.getIncomeByDateRangeSync(userId, startDate, endDate);
                List<Expense> expenseList = expenseRepository.getExpensesByDateRangeSync(userId, startDate, endDate);

                String title = "Budget Report - " + getMonthName(selectedMonth) + " " + selectedYear;
                String path = ExportUtils.exportToPdf(app, incomeList, expenseList, currencySymbol, title);
                exportResult.postValue(path != null ? "PDF:" + path : "ERROR:PDF export failed");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return (month >= 1 && month <= 12) ? months[month - 1] : "";
    }

    public int getSelectedMonth() { return selectedMonth; }
    public int getSelectedYear() { return selectedYear; }
}
