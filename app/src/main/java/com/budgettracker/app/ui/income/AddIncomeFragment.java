package com.budgettracker.app.ui.income;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.budgettracker.app.R;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.databinding.FragmentAddIncomeBinding;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.viewmodel.IncomeViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class AddIncomeFragment extends Fragment {

    private FragmentAddIncomeBinding binding;
    private IncomeViewModel incomeViewModel;
    private long selectedDate = System.currentTimeMillis();
    private Income editingIncome = null;
    private boolean isEditMode = false;
    private boolean resultHandled = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddIncomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        incomeViewModel = new ViewModelProvider(this).get(IncomeViewModel.class);
        resultHandled = false;

        if (getArguments() != null) {
            int incomeId = getArguments().getInt("incomeId", -1);
            if (incomeId != -1) {
                isEditMode = true;
                loadIncomeForEdit(incomeId);
            }
        }

        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
        updateTitle();
        setupClickListeners();
        observeViewModel();
    }

    private void loadIncomeForEdit(int incomeId) {
        new Thread(() -> {
            editingIncome = incomeViewModel.getIncomeById(incomeId);
            if (editingIncome != null && isAdded()) {
                selectedDate = editingIncome.getDate();
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.etAmount.setText(CurrencyUtils.formatPlain(editingIncome.getAmount()));
                        binding.etSource.setText(editingIncome.getSource());
                        binding.etNotes.setText(editingIncome.getNotes());
                        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
                    }
                });
            }
        }).start();
    }

    private void updateTitle() {
        if (isEditMode) {
            binding.tvFormTitle.setText("Edit Income");
            binding.btnSave.setText("Update Income");
        } else {
            binding.tvFormTitle.setText("Add Income");
            binding.btnSave.setText("Save Income");
        }
    }

    private void setupClickListeners() {
        binding.tvSelectedDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> saveIncome());
        binding.btnCancel.setOnClickListener(v -> {
            if (isAdded()) {
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        new DatePickerDialog(requireContext(),
                (v, year, month, day) -> {
                    cal.set(year, month, day);
                    selectedDate = cal.getTimeInMillis();
                    if (binding != null) {
                        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveIncome() {
        if (binding == null) return;

        String amountStr = binding.etAmount.getText().toString().trim();
        String source = binding.etSource.getText().toString().trim();
        String notes = binding.etNotes.getText().toString().trim();

        String amountError = CurrencyUtils.validateAmount(amountStr);
        if (amountError != null) {
            binding.tilAmount.setError(amountError);
            return;
        }
        binding.tilAmount.setError(null);

        if (source.isEmpty()) {
            binding.tilSource.setError("Source is required");
            return;
        }
        binding.tilSource.setError(null);

        double amount = CurrencyUtils.parseAmount(amountStr);

        if (isEditMode && editingIncome != null) {
            editingIncome.setAmount(amount);
            editingIncome.setSource(source);
            editingIncome.setDate(selectedDate);
            editingIncome.setNotes(notes);
            incomeViewModel.updateIncome(editingIncome);
        } else {
            incomeViewModel.addIncome(amount, source, selectedDate, notes);
        }
    }

    private void observeViewModel() {
        incomeViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                binding.btnSave.setEnabled(!loading);
            }
        });

        incomeViewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null || resultHandled) return;

            if (result.startsWith("SUCCESS:")) {
                resultHandled = true;
                // Clear the result so it won't fire again
                incomeViewModel.operationResult.setValue(null);
                if (isAdded() && !requireActivity().isFinishing()) {
                    try {
                        Navigation.findNavController(requireView()).navigateUp();
                    } catch (Exception e) {
                        requireActivity().onBackPressed();
                    }
                }
            } else if (result.startsWith("ERROR:")) {
                if (isAdded() && binding != null) {
                    Snackbar.make(requireView(), result.substring(6), Snackbar.LENGTH_LONG)
                            .setBackgroundTint(requireContext().getColor(R.color.error_red))
                            .show();
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
