package com.chataika.smartpantry.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.data.PantryDbHelper;
import com.chataika.smartpantry.logic.IngredientValidator;
import com.chataika.smartpantry.logic.Quantities;
import com.chataika.smartpantry.logic.UnitConverter;
import com.chataika.smartpantry.model.PantryItem;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Add/Edit Ingredient screen.
 *
 * Started with no extras  -> "Add" mode   (CREATE a new pantry row).
 * Started with EXTRA_ITEM_ID -> "Edit" mode (UPDATE or DELETE that row).
 * On success it returns RESULT_OK with a message the pantry screen shows in a Snackbar.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "com.chataika.smartpantry.EXTRA_ITEM_ID";
    public static final String EXTRA_RESULT_MESSAGE = "com.chataika.smartpantry.EXTRA_RESULT_MESSAGE";
    private static final String STATE_EXPIRY = "state_expiry";

    private PantryDbHelper db;
    private PantryItem existingItem; // null when adding
    private LocalDate selectedExpiry;
    private final List<String> units = new ArrayList<>(Arrays.asList(UnitConverter.PICKER_UNITS));

    private TextInputLayout nameLayout;
    private TextInputLayout quantityLayout;
    private TextInputLayout expiryLayout;
    private MaterialAutoCompleteTextView nameInput;
    private TextInputEditText quantityInput;
    private TextInputEditText expiryInput;
    private Spinner unitSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = PantryDbHelper.getInstance(this);
        bindViews();

        // Read the item id passed in the Intent (-1 means "add new")
        long itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        if (itemId != -1) {
            existingItem = db.getPantryItem(itemId);
            if (existingItem == null) {
                Toast.makeText(this, R.string.item_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (!units.contains(existingItem.getUnit())) {
                units.add(existingItem.getUnit()); // keep any unit saved outside the picker list
            }
        }
        setTitle(existingItem == null ? R.string.title_add_ingredient : R.string.title_edit_ingredient);

        setUpNameSuggestions();
        setUpUnitSpinner();
        setUpExpiryPicker();

        if (savedInstanceState != null) {
            // Rotation: EditTexts restore themselves; we only need to restore the chosen date
            String saved = savedInstanceState.getString(STATE_EXPIRY);
            selectedExpiry = saved == null ? null : LocalDate.parse(saved);
        } else if (existingItem != null) {
            fillForm(existingItem);
        }
        showExpiry();

        findViewById(R.id.button_save).setOnClickListener(v -> save());
        View deleteButton = findViewById(R.id.button_delete);
        deleteButton.setVisibility(existingItem != null ? View.VISIBLE : View.GONE);
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.layout_name);
        quantityLayout = findViewById(R.id.layout_quantity);
        expiryLayout = findViewById(R.id.layout_expiry);
        nameInput = findViewById(R.id.input_name);
        quantityInput = findViewById(R.id.input_quantity);
        expiryInput = findViewById(R.id.input_expiry);
        unitSpinner = findViewById(R.id.spinner_unit);
    }

    /** Suggest ingredient names used by the recipes, which encourages names that will match. */
    private void setUpNameSuggestions() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, db.getKnownIngredientNames());
        nameInput.setAdapter(adapter);
    }

    private void setUpUnitSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);
    }

    private void setUpExpiryPicker() {
        expiryInput.setOnClickListener(v -> {
            LocalDate initial = selectedExpiry != null ? selectedExpiry : LocalDate.now();
            // DatePickerDialog months are 0-based; LocalDate months are 1-based
            DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) -> {
                selectedExpiry = LocalDate.of(year, month + 1, day);
                showExpiry();
            }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
            dialog.show();
        });
        findViewById(R.id.button_clear_date).setOnClickListener(v -> {
            selectedExpiry = null;
            showExpiry();
        });
    }

    private void fillForm(PantryItem item) {
        nameInput.setText(item.getName(), false); // false = don't pop up the suggestions list
        quantityInput.setText(Quantities.format(item.getQuantity()));
        unitSpinner.setSelection(units.indexOf(item.getUnit()));
        selectedExpiry = item.getExpiryLocalDate();
    }

    private void showExpiry() {
        expiryInput.setText(selectedExpiry == null ? "" : PantryAdapter.DATE_FORMAT.format(selectedExpiry));
        expiryLayout.setError(null);
        findViewById(R.id.button_clear_date).setVisibility(selectedExpiry == null ? View.GONE : View.VISIBLE);
    }

    /** Validates every field, showing an error message under each invalid one. */
    private boolean validate() {
        boolean valid = true;

        switch (IngredientValidator.validateName(textOf(nameInput.getText()))) {
            case REQUIRED:
                nameLayout.setError(getString(R.string.error_name_required));
                valid = false;
                break;
            case LENGTH:
                nameLayout.setError(getString(R.string.error_name_length));
                valid = false;
                break;
            case INVALID_CHARS:
                nameLayout.setError(getString(R.string.error_name_chars));
                valid = false;
                break;
            default:
                nameLayout.setError(null);
        }

        switch (IngredientValidator.validateQuantity(textOf(quantityInput.getText()))) {
            case REQUIRED:
                quantityLayout.setError(getString(R.string.error_quantity_required));
                valid = false;
                break;
            case INVALID:
                quantityLayout.setError(getString(R.string.error_quantity_invalid));
                valid = false;
                break;
            case TOO_LARGE:
                quantityLayout.setError(getString(R.string.error_quantity_too_large));
                valid = false;
                break;
            default:
                quantityLayout.setError(null);
        }

        // A brand-new item that has already expired is almost certainly a typo
        if (existingItem == null && selectedExpiry != null && selectedExpiry.isBefore(LocalDate.now())) {
            expiryLayout.setError(getString(R.string.error_expiry_past));
            valid = false;
        } else {
            expiryLayout.setError(null);
        }

        return valid;
    }

    private void save() {
        if (!validate()) {
            Toast.makeText(this, R.string.error_fix_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String name = textOf(nameInput.getText()).trim();
        double quantity = IngredientValidator.parseQuantity(textOf(quantityInput.getText()));
        String unit = (String) unitSpinner.getSelectedItem();
        String expiry = selectedExpiry == null ? null : selectedExpiry.toString();

        boolean ok;
        int message;
        if (existingItem == null) {
            ok = db.insertPantryItem(new PantryItem(name, quantity, unit, expiry)) != -1;
            message = R.string.item_saved;
        } else {
            existingItem.setName(name);
            existingItem.setQuantity(quantity);
            existingItem.setUnit(unit);
            existingItem.setExpiryDate(expiry);
            ok = db.updatePantryItem(existingItem);
            message = R.string.item_updated;
        }

        if (!ok) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        finishWithMessage(getString(message));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message, existingItem.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    db.deletePantryItem(existingItem.getId());
                    finishWithMessage(getString(R.string.item_deleted, existingItem.getName()));
                })
                .show();
    }

    /** Sends a result Intent back to the pantry screen and closes this screen. */
    private void finishWithMessage(String message) {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_MESSAGE, message);
        setResult(RESULT_OK, result);
        finish();
    }

    private static String textOf(CharSequence text) {
        return text == null ? "" : text.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXPIRY, selectedExpiry == null ? null : selectedExpiry.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (existingItem != null) {
            getMenuInflater().inflate(R.menu.menu_add_edit, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Toolbar back arrow: just close, returning to the existing pantry screen. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
