package com.budgettracker.app.ui.income;

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
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;

/**
 * RecyclerView adapter for income list.
 * Uses DiffUtil for efficient updates.
 */
public class IncomeAdapter extends ListAdapter<Income, IncomeAdapter.IncomeViewHolder> {

    public interface OnEditClickListener {
        void onEdit(Income income);
    }

    public interface OnDeleteClickListener {
        void onDelete(Income income);
    }

    private final OnEditClickListener editListener;
    private final OnDeleteClickListener deleteListener;
    private final String currencySymbol;

    public IncomeAdapter(OnEditClickListener editListener,
                         OnDeleteClickListener deleteListener,
                         String currencySymbol) {
        super(DIFF_CALLBACK);
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.currencySymbol = currencySymbol;
    }

    @NonNull
    @Override
    public IncomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_income, parent, false);
        return new IncomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncomeViewHolder holder, int position) {
        Income income = getItem(position);
        holder.tvSource.setText(income.getSource());
        holder.tvAmount.setText(CurrencyUtils.format(income.getAmount(), currencySymbol));
        holder.tvDate.setText(DateUtils.formatDate(income.getDate()));
        holder.tvNotes.setText(income.getNotes() != null && !income.getNotes().isEmpty()
                ? income.getNotes() : "");
        holder.tvNotes.setVisibility(
                (income.getNotes() != null && !income.getNotes().isEmpty())
                        ? View.VISIBLE : View.GONE);

        holder.btnEdit.setOnClickListener(v -> editListener.onEdit(income));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(income));
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView tvSource, tvAmount, tvDate, tvNotes;
        ImageButton btnEdit, btnDelete;

        IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSource = itemView.findViewById(R.id.tv_income_source);
            tvAmount = itemView.findViewById(R.id.tv_income_amount);
            tvDate = itemView.findViewById(R.id.tv_income_date);
            tvNotes = itemView.findViewById(R.id.tv_income_notes);
            btnEdit = itemView.findViewById(R.id.btn_edit_income);
            btnDelete = itemView.findViewById(R.id.btn_delete_income);
        }
    }

    private static final DiffUtil.ItemCallback<Income> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Income>() {
                @Override
                public boolean areItemsTheSame(@NonNull Income oldItem, @NonNull Income newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Income oldItem, @NonNull Income newItem) {
                    return oldItem.getAmount() == newItem.getAmount()
                            && oldItem.getSource().equals(newItem.getSource())
                            && oldItem.getDate() == newItem.getDate();
                }
            };
}
