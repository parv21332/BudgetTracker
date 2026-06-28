package com.budgettracker.app.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.budgettracker.app.R;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;

/**
 * RecyclerView adapter for expense list with DiffUtil.
 */
public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder> {

    public interface OnEditClickListener { void onEdit(Expense expense); }
    public interface OnDeleteClickListener { void onDelete(Expense expense); }

    private final OnEditClickListener editListener;
    private final OnDeleteClickListener deleteListener;
    private final String currencySymbol;

    public ExpenseAdapter(OnEditClickListener editListener,
                          OnDeleteClickListener deleteListener,
                          String currencySymbol) {
        super(DIFF_CALLBACK);
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.currencySymbol = currencySymbol;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = getItem(position);
        holder.tvCategory.setText(expense.getCategoryName());
        holder.tvAmount.setText("- " + CurrencyUtils.format(expense.getAmount(), currencySymbol));
        holder.tvDate.setText(DateUtils.formatDate(expense.getDate()));
        holder.tvNotes.setText(expense.getNotes() != null ? expense.getNotes() : "");
        holder.tvNotes.setVisibility(
                (expense.getNotes() != null && !expense.getNotes().isEmpty())
                        ? View.VISIBLE : View.GONE);

        holder.btnEdit.setOnClickListener(v -> editListener.onEdit(expense));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(expense));
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvDate, tvNotes;
        ImageButton btnEdit, btnDelete;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_expense_category);
            tvAmount = itemView.findViewById(R.id.tv_expense_amount);
            tvDate = itemView.findViewById(R.id.tv_expense_date);
            tvNotes = itemView.findViewById(R.id.tv_expense_notes);
            btnEdit = itemView.findViewById(R.id.btn_edit_expense);
            btnDelete = itemView.findViewById(R.id.btn_delete_expense);
        }
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense a, @NonNull Expense b) {
                    return a.getId() == b.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Expense a, @NonNull Expense b) {
                    return a.getAmount() == b.getAmount()
                            && a.getCategoryName().equals(b.getCategoryName())
                            && a.getDate() == b.getDate();
                }
            };
}
