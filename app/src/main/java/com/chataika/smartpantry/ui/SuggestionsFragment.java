package com.chataika.smartpantry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chataika.smartpantry.R;
import com.chataika.smartpantry.data.AppSettings;
import com.chataika.smartpantry.data.PantryDbHelper;
import com.chataika.smartpantry.data.PantryMatching;
import com.chataika.smartpantry.logic.MatchResult;
import com.chataika.smartpantry.logic.RecipeMatcher;
import com.chataika.smartpantry.model.Recipe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggested Recipes screen. Runs the strict-matching rule against the current pantry and
 * lists ONLY recipes the user can cook right now. If enabled in Settings, recipes missing
 * exactly one ingredient appear underneath in a separate, clearly labelled section.
 */
public class SuggestionsFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private RecipeAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyLayout;
    private TextView emptyText;
    private TextView summaryText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_recipes);
        emptyLayout = view.findViewById(R.id.layout_empty);
        emptyText = view.findViewById(R.id.text_empty);
        summaryText = view.findViewById(R.id.text_summary);

        adapter = new RecipeAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_go_to_pantry).setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_pantry));
    }

    /** Re-run matching whenever the screen is shown, as the pantry may have changed. */
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        PantryDbHelper db = PantryDbHelper.getInstance(requireContext());
        AppSettings settings = new AppSettings(requireContext());
        LocalDate today = LocalDate.now();

        List<Recipe> recipes = db.getAllRecipes();
        RecipeMatcher matcher = PantryMatching.createMatcher(requireContext(), today);
        List<MatchResult> results = matcher.matchAll(recipes, today, settings.getExpiryWarningDays());

        List<MatchResult> strict = RecipeMatcher.strictMatches(results);
        List<MatchResult> almost = settings.isShowAlmostThere()
                ? RecipeMatcher.almostThere(results)
                : new ArrayList<MatchResult>();

        List<RecipeAdapter.Row> rows = new ArrayList<>();
        if (!strict.isEmpty()) {
            rows.add(RecipeAdapter.Row.header(getString(R.string.section_ready)));
            for (MatchResult r : strict) rows.add(RecipeAdapter.Row.recipe(r));
        }
        if (!almost.isEmpty()) {
            rows.add(RecipeAdapter.Row.header(getString(R.string.section_almost)));
            for (MatchResult r : almost) rows.add(RecipeAdapter.Row.recipe(r));
        }
        adapter.setRows(rows);

        summaryText.setText(getResources().getQuantityString(
                R.plurals.suggestions_summary, strict.size(), strict.size(), recipes.size()));

        // Friendly message instead of a blank screen when nothing can be cooked.
        // The "almost there" list may still be shown underneath it.
        boolean noStrictMatches = strict.isEmpty();
        emptyLayout.setVisibility(noStrictMatches && almost.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        emptyText.setText(db.getAllPantryItems().isEmpty()
                ? R.string.no_matches_empty_pantry
                : R.string.no_matches);
        if (noStrictMatches && !almost.isEmpty()) {
            summaryText.setText(R.string.no_matches);
        }
    }

    /** Open the detail screen, passing the recipe id in the Intent. */
    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
