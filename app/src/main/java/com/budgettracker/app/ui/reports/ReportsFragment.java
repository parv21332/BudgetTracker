package com.budgettracker.app.ui.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.budgettracker.app.R;
import com.budgettracker.app.data.database.ExpenseDao;
import com.budgettracker.app.data.model.MonthlyReport;
import com.budgettracker.app.databinding.FragmentReportsBinding;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.viewmodel.ReportsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * ReportsFragment - shows monthly and weekly reports with pie and bar charts.
 * Supports PDF and Excel export.
 */
public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel reportsViewModel;
    private final String currencySymbol = "₹";
    private int selectedMonth;
    private int selectedYear;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reportsViewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        selectedMonth = reportsViewModel.getSelectedMonth();
        selectedYear = reportsViewModel.getSelectedYear();

        setupMonthSelector();
        setupTabLayout();
        setupClickListeners();
        observeViewModel();
    }

    private void setupMonthSelector() {
        updateMonthDisplay();

        binding.btnPrevMonth.setOnClickListener(v -> {
            selectedMonth--;
            if (selectedMonth < 1) { selectedMonth = 12; selectedYear--; }
            updateMonthDisplay();
            reportsViewModel.loadReport(selectedMonth, selectedYear);
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            selectedMonth++;
            if (selectedMonth > 12) { selectedMonth = 1; selectedYear++; }
            updateMonthDisplay();
            reportsViewModel.loadReport(selectedMonth, selectedYear);
        });
    }

    private void updateMonthDisplay() {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        binding.tvCurrentMonth.setText(months[selectedMonth - 1] + " " + selectedYear);
    }

    private void setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    reportsViewModel.loadReport(selectedMonth, selectedYear);
                } else {
                    reportsViewModel.loadWeeklyReport();
                }
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }

    private void setupClickListeners() {
        binding.btnExportPdf.setOnClickListener(v ->
                reportsViewModel.exportToPdf(currencySymbol));

        binding.btnExportExcel.setOnClickListener(v ->
                reportsViewModel.exportToExcel(currencySymbol));
    }

    private void observeViewModel() {
        reportsViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        reportsViewModel.currentReport.observe(getViewLifecycleOwner(), report -> {
            if (report != null) updateReportUI(report);
        });

        reportsViewModel.categoryTotals.observe(getViewLifecycleOwner(), totals -> {
            if (totals != null) {
                setupPieChart(totals);
            }
        });

        reportsViewModel.monthlyHistory.observe(getViewLifecycleOwner(), history -> {
            if (history != null) setupBarChart(history);
        });

        reportsViewModel.exportResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.startsWith("PDF:") || result.startsWith("EXCEL:")) {
                String path = result.contains(":") ? result.substring(result.indexOf(":") + 1) : "";
                Snackbar.make(requireView(), "Exported to: " + path, Snackbar.LENGTH_LONG).show();
            } else if (result.startsWith("ERROR:")) {
                Snackbar.make(requireView(), result.substring(6), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(requireContext().getColor(R.color.error_red))
                        .show();
            }
        });
    }

    private void updateReportUI(MonthlyReport report) {
        binding.tvReportIncome.setText(CurrencyUtils.format(report.getTotalIncome(), currencySymbol));
        binding.tvReportExpense.setText(CurrencyUtils.format(report.getTotalExpense(), currencySymbol));
        binding.tvReportBalance.setText(CurrencyUtils.format(report.getBalance(), currencySymbol));

        int balanceColor = report.getBalance() >= 0
                ? requireContext().getColor(R.color.income_green)
                : requireContext().getColor(R.color.expense_red);
        binding.tvReportBalance.setTextColor(balanceColor);

        binding.tvTransactionCount.setText(report.getTransactionCount() + " transactions");
    }

    private void setupPieChart(List<ExpenseDao.CategoryTotal> totals) {
        if (totals.isEmpty()) {
            binding.pieChart.setVisibility(View.GONE);
            binding.tvNoPieData.setVisibility(View.VISIBLE);
            return;
        }
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.tvNoPieData.setVisibility(View.GONE);

        List<PieEntry> entries = new ArrayList<>();
        for (ExpenseDao.CategoryTotal cat : totals) {
            if (cat.total > 0) {
                entries.add(new PieEntry((float) cat.total, cat.category_name));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expenses by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setCenterText("Expenses");
        binding.pieChart.setHoleRadius(40f);
        binding.pieChart.animateY(800);
        binding.pieChart.invalidate();
    }

    private void setupBarChart(List<MonthlyReport> history) {
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            MonthlyReport r = history.get(i);
            incomeEntries.add(new BarEntry(i, (float) r.getTotalIncome()));
            expenseEntries.add(new BarEntry(i, (float) r.getTotalExpense()));
        }

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "Income");
        incomeDataSet.setColor(requireContext().getColor(R.color.income_green));

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "Expense");
        expenseDataSet.setColor(requireContext().getColor(R.color.expense_red));

        BarData barData = new BarData(incomeDataSet, expenseDataSet);
        barData.setBarWidth(0.3f);

        binding.barChart.setData(barData);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.groupBars(0f, 0.2f, 0.05f);
        binding.barChart.animateY(800);
        binding.barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
