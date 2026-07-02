package com.budgettracker.app.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.FragmentDashboardBinding;
import com.budgettracker.app.utils.AppPrefs;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.viewmodel.DashboardViewModel;

/**
 * DashboardFragment - home screen.
 * Displays total income, expense, balance, recent transactions.
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private RecentTransactionAdapter incomeAdapter;
    private RecentTransactionAdapter expenseAdapter;
    private String currencySymbol = "₹";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Load user info
        String name = AppPrefs.getDisplayName(requireContext());
        binding.tvGreeting.setText(getGreeting());
        binding.tvUserName.setText("Hello, " + name + "!");

        // Avatar initial
        if (name != null && !name.isEmpty()) {
            binding.tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        // Month label
        String monthLabel = new java.text.SimpleDateFormat("MMMM yyyy",
                java.util.Locale.getDefault()).format(new java.util.Date());
        binding.tvMonthLabel.setText(monthLabel);
        binding.tvThisMonthLabel.setText(monthLabel);

        setupRecyclerViews();
        observeViewModel();
        setupClickListeners();
    }

    private void setupRecyclerViews() {
        incomeAdapter = new RecentTransactionAdapter(true, currencySymbol);
        binding.rvRecentIncome.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentIncome.setAdapter(incomeAdapter);
        binding.rvRecentIncome.setNestedScrollingEnabled(false);

        expenseAdapter = new RecentTransactionAdapter(false, currencySymbol);
        binding.rvRecentExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentExpenses.setAdapter(expenseAdapter);
        binding.rvRecentExpenses.setNestedScrollingEnabled(false);
    }

    private void observeViewModel() {
        dashboardViewModel.totalIncome.observe(getViewLifecycleOwner(), income ->
                binding.tvTotalIncome.setText(CurrencyUtils.format(income, currencySymbol)));

        dashboardViewModel.totalExpense.observe(getViewLifecycleOwner(), expense ->
                binding.tvTotalExpense.setText(CurrencyUtils.format(expense, currencySymbol)));

        dashboardViewModel.balance.observe(getViewLifecycleOwner(), balance -> {
            binding.tvBalance.setText(CurrencyUtils.format(balance, currencySymbol));
            // balance stays white on the dark gradient header
        });

        dashboardViewModel.monthlyIncome.observe(getViewLifecycleOwner(), income ->
                binding.tvMonthlyIncome.setText(CurrencyUtils.format(income, currencySymbol)));

        dashboardViewModel.monthlyExpense.observe(getViewLifecycleOwner(), expense ->
                binding.tvMonthlyExpense.setText(CurrencyUtils.format(expense, currencySymbol)));

        dashboardViewModel.recentIncome.observe(getViewLifecycleOwner(), incomeList -> {
            incomeAdapter.submitIncomeList(incomeList);
            binding.tvNoRecentIncome.setVisibility(
                    (incomeList == null || incomeList.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        dashboardViewModel.recentExpenses.observe(getViewLifecycleOwner(), expenseList -> {
            expenseAdapter.submitExpenseList(expenseList);
            binding.tvNoRecentExpenses.setVisibility(
                    (expenseList == null || expenseList.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    private void setupClickListeners() {
        binding.fabAddIncome.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_dashboardFragment_to_addIncomeFragment));

        binding.fabAddExpense.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_dashboardFragment_to_addExpenseFragment));

        binding.btnViewAllIncome.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.incomeFragment));

        binding.btnViewAllExpenses.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.expenseFragment));
    }

    @Override
    public void onResume() {
        super.onResume();
        dashboardViewModel.loadStats();
    }

    private String getGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning 👋";
        else if (hour < 17) return "Good Afternoon 👋";
        else return "Good Evening 👋";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
