package com.example.expenses;

import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionCard extends RecyclerView.Adapter<TransactionCard.ViewHolder>{

    private List<Expense> localDataSet;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPurpose;
        private final TextView tvCategory;
        private final TextView tvTime;
        private final TextView tvAmount;
        private final CardView cardView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            tvPurpose = view.findViewById(R.id.tvPurpose);
            tvCategory = view.findViewById(R.id.tvCategory);
            tvTime = view.findViewById(R.id.tvTime);
            tvAmount = view.findViewById(R.id.tvAmount);
            cardView = view.findViewById(R.id.cardView);
        }

        public TextView getTvPurpose() {
            return tvPurpose;
        }

        public TextView getTvCategory() {
            return tvCategory;
        }

        public TextView getTvTime() {
            return tvTime;
        }

        public TextView getTvAmount() {
            return tvAmount;
        }

        public CardView getCardView() {
            return cardView;
        }

    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet ArrayList<Expense> containing the data to populate views to be used
     * by RecyclerView
     */
    public TransactionCard(List<Expense> dataSet) {
        localDataSet = dataSet;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.transaction_card, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        Context cont = viewHolder.itemView.getContext();
        Expense local = localDataSet.get(localDataSet.size() - position - 1);
        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        Log.d("Transaction Card", local.purpose);

        viewHolder.getTvPurpose().setText(cont.getString(R.string.transactionPurpose, local.purpose));
        viewHolder.getTvCategory().setText(cont.getString(R.string.transactionCategory, local.category));
        long timeStamp = local.timeStamp;
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(timeStamp));
        viewHolder.getTvTime().setText(cont.getString(R.string.transactionTime, formattedTime));
        viewHolder.getTvAmount().setText(cont.getString(R.string.transactionAmount, local.amount));
        viewHolder.getCardView().setOnClickListener(v -> {
            // Create a Uri from an intent string. Use the result to create an Intent.
            Uri gmmIntentUri = Uri.parse(String.format(Locale.getDefault(),"geo:%.6f,%.6f?z=16?q=%.6f,%.6f", local.latitude, local.longitude, local.latitude, local.longitude));

            // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            // Make the Intent explicit by setting the Google Maps package
            mapIntent.setPackage("com.google.android.apps.maps");

            // Attempt to start an activity that can handle the Intent
            cont.startActivity(mapIntent);
        });
        viewHolder.getCardView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d("Transaction Card", "Hello Long Press");
                return true;
            }
        });
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        Log.d("Transaction Card Item Count", String.valueOf(localDataSet.size()));
        return localDataSet.size();
    }
}
