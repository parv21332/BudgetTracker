package com.budgettracker.app.ui.income;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.budgettracker.app.R;
import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.databinding.FragmentAddIncomeBinding;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class AddIncomeFragment extends Fragment {

    private FragmentAddIncomeBinding binding;
    private long selectedDate = System.currentTimeMillis();
    private Income editingIncome = null;
    private boolean isEditMode = false;

    // Store context and userId before going to background thread
    private Context appContext;
    private int currentUserId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddIncomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get context and userId on main thread
        appContext = requireContext().getApplicationContext();
        currentUserId = new SessionManager(appContext).getUserId();

        if (getArguments() != null) {
            int incomeId = getArguments().getInt("incomeId", -1);
            if (incomeId != -1) {
                isEditMode = true;
                loadForEdit(incomeId);
            }
        }

        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
        if (isEditMode) {
            binding.tvFormTitle.setText("Edit Income");
            binding.btnSave.setText("Update");
        }

        binding.tvSelectedDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.btnSave.setOnClickListener(v -> saveIncome());
    }

    private void goBack() {
        if (isAdded() && getView() != null) {
            try {
                Navigation.findNavController(requireView()).navigateUp();
            } catch (Exception e) {
                requireActivity().onBackPressed();
            }
        }
    }

    private void loadForEdit(int incomeId) {
        // Use application context — safe for background thread
        final Context ctx = appContext;
        new Thread(() -> {
            try {
                Income inc = BudgetDatabase.getDatabase(ctx).incomeDao().getIncomeById(incomeId);
                if (inc != null) {
                    editingIncome = inc;
                    selectedDate  = inc.getDate();
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        if (binding == null) return;
                        binding.etAmount.setText(CurrencyUtils.formatPlain(inc.getAmount()));
                        binding.etSource.setText(inc.getSource());
                        binding.etNotes.setText(inc.getNotes() != null ? inc.getNotes() : "");
                        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate);
        new DatePickerDialog(requireContext(), (v, y, m, d) -> {
            cal.set(y, m, d);
            selectedDate = cal.getTimeInMillis();
            if (binding != null) binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveIncome() {
        if (binding == null) return;

        String amountStr = binding.etAmount.getText().toString().trim();
        String source    = binding.etSource.getText().toString().trim();
        String notes     = binding.etNotes.getText().toString().trim();

        // --- Validate ---
        String amtErr = CurrencyUtils.validateAmount(amountStr);
        if (amtErr != null) {
            binding.tilAmount.setError(amtErr);
            return;
        }
        binding.tilAmount.setError(null);

        if (source.isEmpty()) {
            binding.tilSource.setError("Source is required");
            return;
        }
        binding.tilSource.setError(null);

        if (currentUserId <= 0) {
            showSnack("Session error. Please logout and login again.", true);
            return;
        }

        // --- UI feedback ---
        binding.btnSave.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Capture all values on main thread before going background
        final double  amount   = CurrencyUtils.parseAmount(amountStr);
        final String  srcFinal = source;
        final String  notesFin = notes;
        final long    dateFin  = selectedDate;
        final int     uid      = currentUserId;
        final Context ctx      = appContext;
        final boolean editMode = isEditMode;
        final Income  editing  = editingIncome;

        new Thread(() -> {
            try {
                BudgetDatabase db = BudgetDatabase.getDatabase(ctx);

                if (editMode && editing != null) {
                    editing.setAmount(amount);
                    editing.setSource(srcFinal);
                    editing.setDate(dateFin);
                    editing.setNotes(notesFin);
                    editing.setUpdatedAt(System.currentTimeMillis());
                    db.incomeDao().updateIncome(editing);
                } else {
                    Income income = new Income(uid, amount, srcFinal, dateFin, notesFin);
                    long insertedId = db.incomeDao().insertIncome(income);
                    if (insertedId <= 0) {
                        throw new Exception("DB insert failed, id=" + insertedId);
                    }
                }

                // Success — back to list on main thread
                if (isAdded()) requireActivity().runOnUiThread(this::goBack);

            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                    }
                    showSnack("Error: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void showSnack(String msg, boolean isError) {
        if (!isAdded() || binding == null) return;
        Snackbar sb = Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG);
        if (isError) sb.setBackgroundTint(requireContext().getColor(R.color.error_red));
        sb.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
