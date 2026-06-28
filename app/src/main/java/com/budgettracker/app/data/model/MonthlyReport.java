package com.budgettracker.app.data.model;

/**
 * Non-entity POJO used for monthly report queries.
 * Used with Room @Query to aggregate data by month/year.
 */
public class MonthlyReport {

    private int month;
    private int year;
    private double totalIncome;
    private double totalExpense;
    private double balance;
    private int transactionCount;

    public MonthlyReport() {}

    public MonthlyReport(int month, int year, double totalIncome,
                         double totalExpense, int transactionCount) {
        this.month = month;
        this.year = year;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = totalIncome - totalExpense;
        this.transactionCount = transactionCount;
    }

    // Getters and Setters
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
        this.balance = this.totalIncome - this.totalExpense;
    }

    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
        this.balance = this.totalIncome - this.totalExpense;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public String getMonthName() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        if (month >= 1 && month <= 12) return months[month - 1];
        return "";
    }
}
