package com.example.smartbugdet.ui.transactions;

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

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionListAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private Context context;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat isoParser;
    private SimpleDateFormat dateFormatter;

    public TransactionListAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Parser for the incoming ISO 8601 date string
        this.isoParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        this.isoParser.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Formatter for the user-facing date string
        this.dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvTransactionName.setText(transaction.getTitle());

        // Set Amount and Color
        String formattedAmount;
        int amountColor;
        if ("expense".equalsIgnoreCase(transaction.getType())) {
            formattedAmount = String.format(Locale.getDefault(), "- %s", currencyFormatter.format(transaction.getAmount()));
            amountColor = ContextCompat.getColor(context, R.color.expense_red);
        } else {
            formattedAmount = String.format(Locale.getDefault(), "+ %s", currencyFormatter.format(transaction.getAmount()));
            amountColor = ContextCompat.getColor(context, R.color.income_green);
        }
        holder.tvTransactionAmount.setText(formattedAmount);
        holder.tvTransactionAmount.setTextColor(amountColor);

        // Set Previous Balance
        double preAmount = transaction.getPreAmount();
        String balanceBeforeText = String.format(Locale.getDefault(), "Prev. Bal: %s", currencyFormatter.format(preAmount));
        holder.tvBalanceBeforeTransaction.setText(balanceBeforeText);

        // Set Date
        if (transaction.getDate() != null) {
            holder.tvTransactionDate.setText(formatDate(transaction.getDate()));
        }
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    public void updateData(List<Transaction> newTransactionList) {
        this.transactionList.clear();
        if (newTransactionList != null) {
            this.transactionList.addAll(newTransactionList);
        }
        notifyDataSetChanged();
    }

    private String formatDate(String dateString) {
        try {
            Date date = isoParser.parse(dateString);
            return dateFormatter.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // Fallback to original string on error
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionName;
        TextView tvTransactionAmount;
        TextView tvTransactionDate;
        TextView tvBalanceBeforeTransaction;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionName = itemView.findViewById(R.id.tv_transaction_name);
            tvTransactionAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvTransactionDate = itemView.findViewById(R.id.tv_transaction_date);
            tvBalanceBeforeTransaction = itemView.findViewById(R.id.tv_balance_before_transaction);
        }
    }
}
