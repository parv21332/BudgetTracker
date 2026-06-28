package com.budgettracker.app.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.budgettracker.app.R;
import com.budgettracker.app.data.model.Category;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.databinding.FragmentAddExpenseBinding;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.viewmodel.ExpenseViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * AddExpenseFragment - handles both Add and Edit expense.
 */
public class AddExpenseFragment extends Fragment {

    private FragmentAddExpenseBinding binding;
    private ExpenseViewModel expenseViewModel;
    private long selectedDate = System.currentTimeMillis();
    private Expense editingExpense = null;
    private boolean isEditMode = false;
    private List<Category> categoryList = new ArrayList<>();
    private int selectedCategoryIndex = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        if (getArguments() != null) {
            int expenseId = getArguments().getInt("expenseId", -1);
            if (expenseId != -1) {
                isEditMode = true;
                loadExpenseForEdit(expenseId);
            }
        }

        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
        updateTitle();
        setupClickListeners();
        observeViewModel();
    }

    private void loadExpenseForEdit(int expenseId) {
        new Thread(() -> {
            editingExpense = expenseViewModel.getExpenseById(expenseId);
            if (editingExpense != null) {
                selectedDate = editingExpense.getDate();
                requireActivity().runOnUiThread(() -> {
                    binding.etAmount.setText(CurrencyUtils.formatPlain(editingExpense.getAmount()));
                    binding.etNotes.setText(editingExpense.getNotes());
                    binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
                    // Category spinner will be set once categories load
                });
            }
        }).start();
    }

    private void updateTitle() {
        if (isEditMode) {
            binding.tvFormTitle.setText("Edit Expense");
            binding.btnSave.setText("Update Expense");
        } else {
            binding.tvFormTitle.setText("Add Expense");
            binding.btnSave.setText("Save Expense");
        }
    }

    private void setupClickListeners() {
        binding.tvSelectedDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> saveExpense());
        binding.btnCancel.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        new DatePickerDialog(requireContext(),
                (v, year, month, day) -> {
                    cal.set(year, month, day);
                    selectedDate = cal.getTimeInMillis();
                    binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveExpense() {
        String amountStr = binding.etAmount.getText().toString().trim();
        String notes = binding.etNotes.getText().toString().trim();

        String amountError = CurrencyUtils.validateAmount(amountStr);
        if (amountError != null) {
            binding.tilAmount.setError(amountError);
            return;
        }
        binding.tilAmount.setError(null);

        if (categoryList.isEmpty()) {
            Snackbar.make(requireView(), "Please select a category", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double amount = CurrencyUtils.parseAmount(amountStr);
        Category selectedCategory = categoryList.get(
                binding.spinnerCategory.getSelectedItemPosition());

        if (isEditMode && editingExpense != null) {
            editingExpense.setAmount(amount);
            editingExpense.setCategoryId(selectedCategory.getId());
            editingExpense.setCategoryName(selectedCategory.getName());
            editingExpense.setDate(selectedDate);
            editingExpense.setNotes(notes);
            expenseViewModel.updateExpense(editingExpense);
        } else {
            expenseViewModel.addExpense(amount, selectedCategory.getId(),
                    selectedCategory.getName(), selectedDate, notes);
        }
    }

    private void observeViewModel() {
        expenseViewModel.categories.observe(getViewLifecycleOwner(), categories -> {
            if (categories == null || categories.isEmpty()) return;
            categoryList = categories;

            List<String> names = new ArrayList<>();
            for (Category c : categories) names.add(c.getName());

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCategory.setAdapter(spinnerAdapter);

            // Restore selection in edit mode
            if (isEditMode && editingExpense != null) {
                for (int i = 0; i < categoryList.size(); i++) {
                    if (categoryList.get(i).getName().equals(editingExpense.getCategoryName())) {
                        binding.spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }
        });

        expenseViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!loading);
        });

        expenseViewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.startsWith("SUCCESS:")) {
                Snackbar.make(requireView(), result.substring(8), Snackbar.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            } else if (result.startsWith("ERROR:")) {
                Snackbar.make(requireView(), result.substring(6), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(requireContext().getColor(R.color.error_red))
                        .show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
