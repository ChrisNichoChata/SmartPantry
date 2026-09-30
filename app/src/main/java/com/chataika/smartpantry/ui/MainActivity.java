package com.chataika.smartpantry.ui;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.chataika.smartpantry.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Single host activity. The bottom navigation bar swaps between the four main screens,
 * each implemented as a Fragment: Pantry, Suggested Recipes (Cook Now), All Recipes, Settings.
 * Add/Edit Ingredient and Recipe Detail are separate Activities opened with Intents.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showScreen(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            // First launch: show the pantry. After a rotation the FragmentManager
            // restores the current fragment itself, so we must not replace it.
            showScreen(R.id.nav_pantry);
        } else {
            updateTitle(bottomNav.getSelectedItemId());
        }
    }

    /** Lets a fragment switch tabs, e.g. the "Add ingredients" button on an empty suggestions list. */
    public void selectTab(@IdRes int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void showScreen(@IdRes int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_suggestions) {
            fragment = new SuggestionsFragment();
        } else if (itemId == R.id.nav_recipes) {
            fragment = new RecipesFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new PantryFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        updateTitle(itemId);
    }

    private void updateTitle(@IdRes int itemId) {
        int title;
        if (itemId == R.id.nav_suggestions) {
            title = R.string.title_suggestions;
        } else if (itemId == R.id.nav_recipes) {
            title = R.string.title_recipes;
        } else if (itemId == R.id.nav_settings) {
            title = R.string.title_settings;
        } else {
            title = R.string.title_pantry;
        }
        setTitle(title);
    }
}
