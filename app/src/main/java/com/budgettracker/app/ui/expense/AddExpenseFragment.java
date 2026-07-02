package com.budgettracker.app.ui.expense;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.budgettracker.app.R;
import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.model.Category;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.databinding.FragmentAddExpenseBinding;
import com.budgettracker.app.utils.AppPrefs;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;
import com.budgettracker.app.utils.NotificationHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddExpenseFragment extends Fragment {

    private FragmentAddExpenseBinding binding;
    private long selectedDate = System.currentTimeMillis();
    private Expense editingExpense = null;
    private boolean isEditMode = false;
    private List<Category> categoryList = new ArrayList<>();

    private Context appContext;
    private int currentUserId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appContext     = requireContext().getApplicationContext();
        currentUserId  = AppPrefs.USER_ID;

        if (getArguments() != null) {
            int expId = getArguments().getInt("expenseId", -1);
            if (expId != -1) {
                isEditMode = true;
                loadForEdit(expId);
            }
        }

        binding.tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
        if (isEditMode) {
            binding.tvFormTitle.setText("Edit Expense");
            binding.btnSave.setText("Update");
        }

        binding.tvSelectedDate.setOnClickListener(v -> showDatePicker());
        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.btnSave.setOnClickListener(v -> saveExpense());

        loadCategories();
    }

    private void goBack() {
        if (isAdded() && getView() != null) {
            try {
                Navigation.findNavController(requireView()).navigateUp();
            } catch (Exception e) {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        }
    }

    private void loadCategories() {
        final Context ctx = appContext;
        final int uid     = currentUserId;
        new Thread(() -> {
            try {
                // Retry up to 10 times (up to ~2 seconds) to handle the race condition
                // where DB seeding hasn't completed yet on first install.
                List<Category> cats = null;
                for (int attempt = 0; attempt < 10; attempt++) {
                    cats = BudgetDatabase.getDatabase(ctx)
                            .categoryDao().getAllCategoriesSync(uid);
                    if (cats != null && !cats.isEmpty()) break;
                    try { Thread.sleep(200); } catch (InterruptedException ie) { break; }
                }
                final List<Category> finalCats = (cats != null && !cats.isEmpty())
                        ? cats : new ArrayList<>();
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    categoryList = finalCats;
                    List<String> names = new ArrayList<>();
                    for (Category c : categoryList) names.add(c.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerCategory.setAdapter(adapter);

                    if (isEditMode && editingExpense != null) {
                        for (int i = 0; i < categoryList.size(); i++) {
                            if (categoryList.get(i).getName()
                                    .equals(editingExpense.getCategoryName())) {
                                binding.spinnerCategory.setSelection(i);
                                break;
                            }
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadForEdit(int expId) {
        final Context ctx = appContext;
        new Thread(() -> {
            try {
                Expense exp = BudgetDatabase.getDatabase(ctx)
                        .expenseDao().getExpenseById(expId);
                if (exp != null) {
                    editingExpense = exp;
                    selectedDate   = exp.getDate();
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        if (binding == null) return;
                        binding.etAmount.setText(CurrencyUtils.formatPlain(exp.getAmount()));
                        binding.etNotes.setText(exp.getNotes() != null ? exp.getNotes() : "");
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

    private void saveExpense() {
        if (binding == null) return;

        String amountStr = binding.etAmount.getText().toString().trim();
        String notes     = binding.etNotes.getText().toString().trim();

        String amtErr = CurrencyUtils.validateAmount(amountStr);
        if (amtErr != null) {
            binding.tilAmount.setError(amtErr);
            return;
        }
        binding.tilAmount.setError(null);

        if (categoryList.isEmpty()) {
            showSnack("Categories are still loading. Please try again in a moment.", false);
            loadCategories(); // retry load
            return;
        }

        if (currentUserId <= 0) {
            showSnack("Could not identify user.", true);
            return;
        }

        binding.btnSave.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Capture on main thread
        final double   amount   = CurrencyUtils.parseAmount(amountStr);
        final String   notesFin = notes;
        final long     dateFin  = selectedDate;
        final int      uid      = currentUserId;
        final Context  ctx      = appContext;
        final boolean  editMode = isEditMode;
        final Expense  editing  = editingExpense;

        int pos = binding.spinnerCategory.getSelectedItemPosition();
        if (pos < 0 || pos >= categoryList.size()) pos = 0;
        final Category selCat = categoryList.get(pos);

        new Thread(() -> {
            try {
                BudgetDatabase db = BudgetDatabase.getDatabase(ctx);

                if (editMode && editing != null) {
                    editing.setAmount(amount);
                    editing.setCategoryId(selCat.getId());
                    editing.setCategoryName(selCat.getName());
                    editing.setDate(dateFin);
                    editing.setNotes(notesFin);
                    editing.setUpdatedAt(System.currentTimeMillis());
                    db.expenseDao().updateExpense(editing);
                } else {
                    Expense expense = new Expense(uid, amount,
                            selCat.getId(), selCat.getName(), dateFin, notesFin);
                    long insertedId = db.expenseDao().insertExpense(expense);
                    if (insertedId <= 0) {
                        throw new Exception("DB insert failed, id=" + insertedId);
                    }
                }

                // ── Budget limit check ──────────────────────────────────────
                double budgetLimit = AppPrefs.getBudgetLimit(ctx);
                if (budgetLimit > 0) {
                    java.text.SimpleDateFormat mf =
                            new java.text.SimpleDateFormat("MM", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat yf =
                            new java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault());
                    String monthStr = mf.format(new java.util.Date());
                    String yearStr  = yf.format(new java.util.Date());
                    double monthTotal = db.expenseDao()
                            .getMonthlyExpense(AppPrefs.USER_ID, monthStr, yearStr);

                    if (monthTotal >= budgetLimit) {
                        NotificationHelper.sendBudgetExceededNotification(
                                ctx, budgetLimit, monthTotal);
                    } else if (monthTotal >= budgetLimit * 0.8) {
                        // 80 % warning
                        NotificationHelper.sendBudgetWarningNotification(
                                ctx, budgetLimit, monthTotal);
                    }
                }
                // ───────────────────────────────────────────────────────────

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
