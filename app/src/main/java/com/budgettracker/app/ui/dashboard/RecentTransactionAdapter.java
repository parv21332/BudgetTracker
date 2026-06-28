package com.budgettracker.app.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.budgettracker.app.R;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.utils.CurrencyUtils;
import com.budgettracker.app.utils.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for recent income or expense items on Dashboard.
 */
public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    private final boolean isIncome;
    private String currencySymbol;
    private List<Income> incomeList = new ArrayList<>();
    private List<Expense> expenseList = new ArrayList<>();

    public RecentTransactionAdapter(boolean isIncome, String currencySymbol) {
        this.isIncome = isIncome;
        this.currencySymbol = currencySymbol;
    }

    public void submitIncomeList(List<Income> list) {
        this.incomeList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void submitExpenseList(List<Expense> list) {
        this.expenseList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isIncome) {
            Income income = incomeList.get(position);
            holder.tvTitle.setText(income.getSource());
            holder.tvAmount.setText(CurrencyUtils.format(income.getAmount(), currencySymbol));
            holder.tvDate.setText(DateUtils.formatDate(income.getDate()));
            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.income_green));
        } else {
            Expense expense = expenseList.get(position);
            holder.tvTitle.setText(expense.getCategoryName());
            holder.tvAmount.setText("- " + CurrencyUtils.format(expense.getAmount(), currencySymbol));
            holder.tvDate.setText(DateUtils.formatDate(expense.getDate()));
            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getColor(R.color.expense_red));
        }
    }

    @Override
    public int getItemCount() {
        return isIncome ? incomeList.size() : expenseList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_transaction_title);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
        }
    }
}
