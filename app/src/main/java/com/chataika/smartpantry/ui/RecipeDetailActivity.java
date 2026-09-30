package com.chataika.smartpantry.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.data.PantryDbHelper;
import com.chataika.smartpantry.data.PantryMatching;
import com.chataika.smartpantry.logic.MatchResult;
import com.chataika.smartpantry.logic.Quantities;
import com.chataika.smartpantry.logic.RecipeMatcher;
import com.chataika.smartpantry.model.Recipe;
import com.chataika.smartpantry.model.RecipeIngredient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Recipe Detail screen: full ingredient list (each marked ✓ or ✗ against the pantry) and
 * the method. If the recipe is a strict match the user can "cook" it, which subtracts the
 * ingredients from the pantry.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "com.chataika.smartpantry.EXTRA_RECIPE_ID";

    private PantryDbHelper db;
    private Recipe recipe;

    private TextView statusText;
    private LinearLayout ingredientsContainer;
    private View cookButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = PantryDbHelper.getInstance(this);
        recipe = db.getRecipe(getIntent().getLongExtra(EXTRA_RECIPE_ID, -1));
        if (recipe == null) {
            Toast.makeText(this, R.string.recipe_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setTitle(recipe.getName());
        ((TextView) findViewById(R.id.text_detail_name)).setText(recipe.getName());
        ((TextView) findViewById(R.id.text_detail_description)).setText(recipe.getDescription());
        ((TextView) findViewById(R.id.text_detail_meta)).setText(getString(R.string.recipe_meta,
                recipe.getPrepMinutes(), recipe.getIngredients().size()));

        statusText = findViewById(R.id.text_detail_status);
        ingredientsContainer = findViewById(R.id.container_ingredients);
        cookButton = findViewById(R.id.button_cook);
        cookButton.setOnClickListener(v -> confirmCook());

        showSteps(recipe.getSteps());
    }

    /** Re-check against the pantry each time the screen is shown. */
    @Override
    protected void onResume() {
        super.onResume();
        if (recipe != null) showMatch();
    }

    private void showMatch() {
        MatchResult result = PantryMatching.createMatcher(this, LocalDate.now()).match(recipe);

        if (result.isStrictMatch()) {
            statusText.setText(R.string.detail_ready);
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_ok));
            cookButton.setVisibility(View.VISIBLE);
        } else {
            int missing = result.getMissingCount();
            statusText.setText(getResources().getQuantityString(R.plurals.detail_missing, missing, missing));
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_bad));
            cookButton.setVisibility(View.GONE);
        }

        ingredientsContainer.removeAllViews();
        for (MatchResult.IngredientCheck check : result.getChecks()) {
            ingredientsContainer.addView(createLine(describe(check),
                    check.isSatisfied() ? R.color.status_ok : R.color.status_bad));
        }
    }

    private String describe(MatchResult.IngredientCheck check) {
        RecipeIngredient ing = check.getIngredient();
        String amount = Quantities.format(ing.getQuantity(), ing.getUnit());
        switch (check.getStatus()) {
            case HAVE:
                return getString(R.string.check_have, amount, ing.getName());
            case NOT_ENOUGH:
                return getString(R.string.check_not_enough, amount, ing.getName(),
                        Quantities.format(check.getAvailable(), ing.getUnit()));
            case UNIT_MISMATCH:
                return getString(R.string.check_unit_mismatch, amount, ing.getName());
            default:
                return getString(R.string.check_missing, amount, ing.getName());
        }
    }

    private void showSteps(List<String> steps) {
        LinearLayout container = findViewById(R.id.container_steps);
        for (int i = 0; i < steps.size(); i++) {
            container.addView(createLine(getString(R.string.step_format, i + 1, steps.get(i)), 0));
        }
    }

    /** Creates one line of text for the ingredient or method list. colorRes 0 = default colour. */
    private TextView createLine(String text, int colorRes) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        view.setPadding(0, padding, 0, padding);
        if (colorRes != 0) {
            view.setTextColor(ContextCompat.getColor(this, colorRes));
        }
        return view;
    }

    private void confirmCook() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cook_confirm_title, recipe.getName()))
                .setMessage(R.string.cook_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.cook, (dialog, which) -> cook())
                .show();
    }

    /** Subtracts the recipe's ingredients from the pantry (UPDATE / DELETE on pantry rows). */
    private void cook() {
        RecipeMatcher matcher = PantryMatching.createMatcher(this, LocalDate.now());
        try {
            Map<Long, Double> newQuantities = matcher.planConsumption(recipe);
            db.applyConsumption(newQuantities);
            Toast.makeText(this, R.string.cooked, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            // The pantry changed since this screen was drawn
            Toast.makeText(this, R.string.cook_failed, Toast.LENGTH_LONG).show();
        }
        showMatch();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
