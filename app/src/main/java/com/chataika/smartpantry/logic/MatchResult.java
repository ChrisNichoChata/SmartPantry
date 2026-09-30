package com.chataika.smartpantry.logic;

import com.chataika.smartpantry.model.Recipe;
import com.chataika.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/** The outcome of checking one recipe against the pantry, ingredient by ingredient. */
public class MatchResult {

    public enum Status {
        /** The pantry has at least the required quantity. */
        HAVE,
        /** The ingredient is in the pantry, but not enough of it. */
        NOT_ENOUGH,
        /** The ingredient is in the pantry, but in a unit we cannot compare (e.g. pcs vs g). */
        UNIT_MISMATCH,
        /** The ingredient is not in the pantry at all. */
        MISSING
    }

    public static class IngredientCheck {
        private final RecipeIngredient ingredient;
        private final Status status;
        private final double available; // expressed in the recipe ingredient's unit

        IngredientCheck(RecipeIngredient ingredient, Status status, double available) {
            this.ingredient = ingredient;
            this.status = status;
            this.available = available;
        }

        public RecipeIngredient getIngredient() { return ingredient; }
        public Status getStatus() { return status; }
        public double getAvailable() { return available; }
        public boolean isSatisfied() { return status == Status.HAVE; }
    }

    private final Recipe recipe;
    private final List<IngredientCheck> checks;
    private int expiringIngredientCount;

    MatchResult(Recipe recipe, List<IngredientCheck> checks) {
        this.recipe = recipe;
        this.checks = new ArrayList<>(checks);
    }

    public Recipe getRecipe() { return recipe; }
    public List<IngredientCheck> getChecks() { return checks; }

    public int getMissingCount() {
        int missing = 0;
        for (IngredientCheck check : checks) {
            if (!check.isSatisfied()) missing++;
        }
        return missing;
    }

    /** The strict rule: every ingredient present in at least the required quantity. */
    public boolean isStrictMatch() {
        return getMissingCount() == 0;
    }

    /** Exactly one ingredient short - shown separately, never in the strict list. */
    public boolean isAlmostThere() {
        return getMissingCount() == 1;
    }

    /** How many of this recipe's ingredients use pantry items that expire soon. */
    public int getExpiringIngredientCount() { return expiringIngredientCount; }
    void setExpiringIngredientCount(int count) { this.expiringIngredientCount = count; }
}
