package com.example.smartbugdet.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartbugdet.R;
import com.example.smartbugdet.model.Transaction;

import java.text.NumberFormat; // Added for currency formatting
import java.util.List;
import java.util.Locale;

public class HomeTransactionAdapter extends RecyclerView.Adapter<HomeTransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private Context context;
    private NumberFormat currencyFormatter; // Added for formatting preAmount

    public HomeTransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
        // Initialize currency formatter
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvTransactionName.setText(transaction.getTitle());

        String formattedAmount;
        int amountColor;

        if ("expense".equalsIgnoreCase(transaction.getType())) {
            formattedAmount = String.format(Locale.getDefault(), "- %s", currencyFormatter.format(transaction.getAmount()));
            amountColor = ContextCompat.getColor(context, R.color.expense_red);
        } else if ("income".equalsIgnoreCase(transaction.getType())) {
            formattedAmount = String.format(Locale.getDefault(), "+ %s", currencyFormatter.format(transaction.getAmount()));
            amountColor = ContextCompat.getColor(context, R.color.income_green);
        } else {
            formattedAmount = currencyFormatter.format(transaction.getAmount());
            amountColor = ContextCompat.getColor(context, R.color.default_text_color);
        }
        holder.tvTransactionAmount.setText(formattedAmount);
        holder.tvTransactionAmount.setTextColor(amountColor);

        // Get preAmount from transaction and format it
        double preAmount = transaction.getPreAmount();
        String balanceBeforeText = String.format(Locale.getDefault(), "Prev. Bal: %s", currencyFormatter.format(preAmount));
        holder.tvBalanceBeforeTransaction.setText(balanceBeforeText);
    }

    @Override
    public int getItemCount() {
        return transactionList == null ? 0 : transactionList.size();
    }

    public void updateData(List<Transaction> newTransactionList) {
        this.transactionList.clear();
        if (newTransactionList != null) {
            this.transactionList.addAll(newTransactionList);
        }
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionName;
        TextView tvTransactionAmount;
        TextView tvBalanceBeforeTransaction;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionName = itemView.findViewById(R.id.tv_transaction_name);
            tvTransactionAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvBalanceBeforeTransaction = itemView.findViewById(R.id.tv_balance_before_transaction);
        }
    }
}
