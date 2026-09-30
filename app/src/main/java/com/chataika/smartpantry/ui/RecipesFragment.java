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

/** Browse the whole recipe collection, with how many ingredients each one is missing. */
public class RecipesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private RecipeAdapter adapter;
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
        summaryText = view.findViewById(R.id.text_summary);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_recipes);
        adapter = new RecipeAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalDate today = LocalDate.now();
        List<Recipe> recipes = PantryDbHelper.getInstance(requireContext()).getAllRecipes();
        RecipeMatcher matcher = PantryMatching.createMatcher(requireContext(), today);
        int warnDays = new AppSettings(requireContext()).getExpiryWarningDays();

        int ready = 0;
        List<RecipeAdapter.Row> rows = new ArrayList<>();
        for (MatchResult result : matcher.matchAll(recipes, today, warnDays)) {
            if (result.isStrictMatch()) ready++;
            rows.add(RecipeAdapter.Row.recipe(result));
        }
        adapter.setRows(rows);
        summaryText.setText(getString(R.string.recipes_summary, recipes.size(), ready));
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
