package com.budgettracker.app.ui.income;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.budgettracker.app.R;
import com.budgettracker.app.databinding.FragmentIncomeBinding;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.viewmodel.IncomeViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

/**
 * IncomeFragment - lists all income transactions with search and date filter.
 */
public class IncomeFragment extends Fragment {

    private FragmentIncomeBinding binding;
    private IncomeViewModel incomeViewModel;
    private IncomeAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentIncomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        incomeViewModel = new ViewModelProvider(this).get(IncomeViewModel.class);

        setupRecyclerView();
        observeViewModel();
        setupClickListeners();
        setupSearch();
    }

    private void setupRecyclerView() {
        adapter = new IncomeAdapter(
                income -> {
                    // Edit: navigate to AddIncome with income ID
                    Bundle args = new Bundle();
                    args.putInt("incomeId", income.getId());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_incomeFragment_to_addIncomeFragment, args);
                },
                income -> {
                    // Delete with confirmation dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Income")
                            .setMessage("Are you sure you want to delete this income record?")
                            .setPositiveButton("Delete", (d, w) -> {
                                incomeViewModel.deleteIncome(income.getId());
                                Snackbar.make(requireView(), "Income deleted", Snackbar.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                },
                "₹"
        );
        binding.rvIncome.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvIncome.setAdapter(adapter);
    }

    private void observeViewModel() {
        incomeViewModel.allIncome.observe(getViewLifecycleOwner(), incomeList -> {
            adapter.submitList(incomeList);
            binding.tvEmptyState.setVisibility(
                    (incomeList == null || incomeList.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        incomeViewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.startsWith("ERROR:")) {
                Snackbar.make(requireView(), result.substring(6), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(requireContext().getColor(R.color.error_red))
                        .show();
            }
        });
    }

    private void setupClickListeners() {
        binding.fabAddIncome.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_incomeFragment_to_addIncomeFragment));

        // Date filter button
        binding.btnFilterDate.setOnClickListener(v -> showDateRangePicker());

        // Clear filter
        binding.btnClearFilter.setOnClickListener(v -> {
            binding.btnClearFilter.setVisibility(View.GONE);
            incomeViewModel.allIncome.observe(getViewLifecycleOwner(),
                    list -> adapter.submitList(list));
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    incomeViewModel.allIncome.observe(getViewLifecycleOwner(),
                            list -> adapter.submitList(list));
                } else {
                    incomeViewModel.searchIncome(query).observe(getViewLifecycleOwner(),
                            list -> adapter.submitList(list));
                }
            }
        });
    }

    private void showDateRangePicker() {
        Calendar calendar = Calendar.getInstance();
        // Pick start date first, then end date
        new DatePickerDialog(requireContext(),
                (view, startYear, startMonth, startDay) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(startYear, startMonth, startDay, 0, 0, 0);
                    new DatePickerDialog(requireContext(),
                            (view2, endYear, endMonth, endDay) -> {
                                Calendar end = Calendar.getInstance();
                                end.set(endYear, endMonth, endDay, 23, 59, 59);
                                incomeViewModel.filterByDateRange(
                                        start.getTimeInMillis(),
                                        end.getTimeInMillis()
                                ).observe(getViewLifecycleOwner(),
                                        list -> adapter.submitList(list));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
