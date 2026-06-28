package com.budgettracker.app.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.budgettracker.app.R;
import com.budgettracker.app.data.model.Category;
import com.budgettracker.app.databinding.FragmentExpenseBinding;
import com.budgettracker.app.viewmodel.ExpenseViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ExpenseFragment - lists all expenses with search, category filter and date filter.
 */
public class ExpenseFragment extends Fragment {

    private FragmentExpenseBinding binding;
    private ExpenseViewModel expenseViewModel;
    private ExpenseAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        setupRecyclerView();
        observeViewModel();
        setupClickListeners();
        setupSearch();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(
                expense -> {
                    Bundle args = new Bundle();
                    args.putInt("expenseId", expense.getId());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_expenseFragment_to_addExpenseFragment, args);
                },
                expense -> new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setPositiveButton("Delete", (d, w) -> {
                            expenseViewModel.deleteExpense(expense.getId());
                            Snackbar.make(requireView(), "Expense deleted", Snackbar.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show(),
                "₹"
        );
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void observeViewModel() {
        expenseViewModel.allExpenses.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            binding.tvEmptyState.setVisibility(
                    (list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        expenseViewModel.categories.observe(getViewLifecycleOwner(), categories -> {
            this.categoryList = categories != null ? categories : new ArrayList<>();
        });
    }

    private void setupClickListeners() {
        binding.fabAddExpense.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_expenseFragment_to_addExpenseFragment));

        binding.btnFilterCategory.setOnClickListener(v -> showCategoryPicker());
        binding.btnFilterDate.setOnClickListener(v -> showDateRangePicker());
        binding.btnClearFilter.setOnClickListener(v -> {
            binding.btnClearFilter.setVisibility(View.GONE);
            expenseViewModel.allExpenses.observe(getViewLifecycleOwner(),
                    list -> adapter.submitList(list));
        });
    }

    private void showCategoryPicker() {
        if (categoryList.isEmpty()) return;
        List<String> names = categoryList.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        String[] nameArray = names.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Filter by Category")
                .setItems(nameArray, (dialog, which) -> {
                    String selected = nameArray[which];
                    expenseViewModel.filterByCategory(selected)
                            .observe(getViewLifecycleOwner(), list -> adapter.submitList(list));
                    binding.btnClearFilter.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDateRangePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, startYear, startMonth, startDay) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(startYear, startMonth, startDay, 0, 0, 0);
                    new DatePickerDialog(requireContext(),
                            (view2, endYear, endMonth, endDay) -> {
                                Calendar end = Calendar.getInstance();
                                end.set(endYear, endMonth, endDay, 23, 59, 59);
                                expenseViewModel.filterByDateRange(
                                        start.getTimeInMillis(),
                                        end.getTimeInMillis()
                                ).observe(getViewLifecycleOwner(), list -> adapter.submitList(list));
                                binding.btnClearFilter.setVisibility(View.VISIBLE);
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    ).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    expenseViewModel.allExpenses.observe(getViewLifecycleOwner(),
                            list -> adapter.submitList(list));
                } else {
                    expenseViewModel.searchExpenses(query).observe(getViewLifecycleOwner(),
                            list -> adapter.submitList(list));
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
