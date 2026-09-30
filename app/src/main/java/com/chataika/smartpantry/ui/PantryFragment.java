package com.chataika.smartpantry.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.data.AppSettings;
import com.chataika.smartpantry.data.PantryDbHelper;
import com.chataika.smartpantry.model.PantryItem;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.List;

/** Pantry List screen: shows every ingredient from the database (the "Read" in CRUD). */
public class PantryFragment extends Fragment implements PantryAdapter.Listener {

    private PantryDbHelper db;
    private AppSettings settings;
    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView bannerView;

    /**
     * Starts AddEditIngredientActivity and receives its result. The result Intent carries a
     * message ("Ingredient saved" etc.) which we show in a Snackbar.
     */
    private final ActivityResultLauncher<Intent> addEditLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String message = result.getData().getStringExtra(AddEditIngredientActivity.EXTRA_RESULT_MESSAGE);
                    if (message != null && getView() != null) {
                        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pantry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = PantryDbHelper.getInstance(requireContext());
        settings = new AppSettings(requireContext());

        recyclerView = view.findViewById(R.id.recycler_pantry);
        emptyView = view.findViewById(R.id.text_empty);
        bannerView = view.findViewById(R.id.text_expiry_banner);

        adapter = new PantryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // CREATE: open the form with no item id
        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                addEditLauncher.launch(new Intent(requireContext(), AddEditIngredientActivity.class)));
    }

    /**
     * Called every time the screen becomes visible again - including when the user comes back
     * from the Add/Edit screen or from cooking a recipe - so the list is always current.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadPantry();
    }

    private void loadPantry() {
        LocalDate today = LocalDate.now();
        List<PantryItem> items = db.getAllPantryItems();
        int warnDays = settings.getExpiryWarningDays();
        adapter.setItems(items, today, settings.isExpiryAlertsEnabled(), warnDays);

        boolean empty = items.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        updateBanner(items, today, warnDays);
    }

    /** Expiring-soon alert banner (can be switched off in Settings). */
    private void updateBanner(List<PantryItem> items, LocalDate today, int warnDays) {
        int expiring = 0;
        int expired = 0;
        for (PantryItem item : items) {
            if (item.isExpired(today)) expired++;
            else if (item.isExpiringWithin(today, warnDays)) expiring++;
        }

        String text = null;
        if (settings.isExpiryAlertsEnabled() && expiring > 0) {
            text = getResources().getQuantityString(R.plurals.expiring_banner, expiring, expiring, warnDays);
        }
        if (expired > 0 && settings.isIgnoreExpired()) {
            String expiredText = getResources().getQuantityString(R.plurals.expired_banner, expired, expired);
            text = text == null ? expiredText : text + "\n" + expiredText;
        }
        bannerView.setText(text);
        bannerView.setVisibility(text == null ? View.GONE : View.VISIBLE);
    }

    /** UPDATE: open the form with this item's id passed as an Intent extra. */
    @Override
    public void onItemClick(PantryItem item) {
        Intent intent = new Intent(requireContext(), AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_ITEM_ID, item.getId());
        addEditLauncher.launch(intent);
    }

    /** DELETE: confirm first, then remove the row and refresh the list. */
    @Override
    public void onDeleteClick(PantryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message, item.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.deletePantryItem(item.getId());
                    loadPantry();
                    Snackbar.make(requireView(), getString(R.string.item_deleted, item.getName()),
                            Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }
}
