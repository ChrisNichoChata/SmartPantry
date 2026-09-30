package com.chataika.smartpantry.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.logic.Quantities;
import com.chataika.smartpantry.model.PantryItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom RecyclerView adapter for the pantry list. The RecyclerView only creates enough
 * ViewHolders to fill the screen and re-binds them with new data as the user scrolls.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    /** Callbacks to the fragment, so the adapter does not need to know about the database. */
    public interface Listener {
        void onItemClick(PantryItem item);
        void onDeleteClick(PantryItem item);
    }

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final Listener listener;
    private final List<PantryItem> items = new ArrayList<>();
    private LocalDate today = LocalDate.now();
    private boolean highlightExpiring = true;
    private int expiringDays = 3;

    public PantryAdapter(Listener listener) {
        this.listener = listener;
    }

    /** Replaces the displayed list, e.g. after the pantry is reloaded from the database. */
    public void setItems(List<PantryItem> newItems, LocalDate today, boolean highlightExpiring, int expiringDays) {
        items.clear();
        items.addAll(newItems);
        this.today = today;
        this.highlightExpiring = highlightExpiring;
        this.expiringDays = expiringDays;
        notifyDataSetChanged(); // small list, so a full refresh is simplest
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PantryItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(item.getName());
        holder.quantity.setText(Quantities.format(item.getQuantity(), item.getUnit()));
        bindExpiry(holder, item, context);

        holder.delete.setContentDescription(context.getString(R.string.delete_item_description, item.getName()));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.delete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    private void bindExpiry(ViewHolder holder, PantryItem item, Context context) {
        TextView view = holder.expiry;
        // Rows are recycled, so always reset the colour before (maybe) highlighting
        view.setTextColor(holder.defaultExpiryColors);

        LocalDate expiry = item.getExpiryLocalDate();
        if (expiry == null) {
            view.setText(R.string.no_expiry);
            return;
        }

        long days = ChronoUnit.DAYS.between(today, expiry);
        if (days < 0) {
            view.setText(context.getString(R.string.expired_on, DATE_FORMAT.format(expiry)));
            // Expired items are always shown in red
            view.setTextColor(ContextCompat.getColor(context, R.color.status_bad));
            return;
        }

        if (days == 0) {
            view.setText(R.string.expires_today);
        } else if (days <= expiringDays) {
            view.setText(context.getResources().getQuantityString(R.plurals.expires_in_days, (int) days, (int) days));
        } else {
            view.setText(context.getString(R.string.expires_on, DATE_FORMAT.format(expiry)));
        }

        // Orange highlight is the "expiring-soon alert" the user can switch off in Settings
        if (highlightExpiring && days <= expiringDays) {
            view.setTextColor(ContextCompat.getColor(context, R.color.status_warning));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Holds references to one row's views so findViewById is only called once per row. */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView quantity;
        final TextView expiry;
        final ImageButton delete;
        final ColorStateList defaultExpiryColors;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            quantity = itemView.findViewById(R.id.text_quantity);
            expiry = itemView.findViewById(R.id.text_expiry);
            delete = itemView.findViewById(R.id.button_delete);
            defaultExpiryColors = expiry.getTextColors();
        }
    }
}
