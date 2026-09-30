package com.chataika.smartpantry.data;

import android.content.Context;

import com.chataika.smartpantry.logic.RecipeMatcher;
import com.chataika.smartpantry.model.PantryItem;

import java.time.LocalDate;
import java.util.List;

/**
 * Builds a {@link RecipeMatcher} from the current database contents and user settings, so
 * the Suggestions, All Recipes and Recipe Detail screens always apply exactly the same rules.
 */
public final class PantryMatching {

    private PantryMatching() { }

    public static RecipeMatcher createMatcher(Context context, LocalDate today) {
        List<PantryItem> pantry = PantryDbHelper.getInstance(context).getAllPantryItems();
        if (new AppSettings(context).isIgnoreExpired()) {
            pantry = RecipeMatcher.withoutExpired(pantry, today);
        }
        return new RecipeMatcher(pantry);
    }
}
