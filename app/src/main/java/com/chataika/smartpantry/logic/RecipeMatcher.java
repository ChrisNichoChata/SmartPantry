package com.chataika.smartpantry.logic;

import com.chataika.smartpantry.model.PantryItem;
import com.chataika.smartpantry.model.Recipe;
import com.chataika.smartpantry.model.RecipeIngredient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business logic: decides which recipes can be cooked from the current pantry.
 *
 * STRICT RULE: a recipe is suggested only if EVERY ingredient it needs is in the pantry
 * in at least the required quantity. Names are compared via {@link IngredientNormalizer}
 * (so "tomatoes" matches "tomato") and amounts via {@link UnitConverter} (so 1 kg covers 500 g).
 *
 * Pure Java - no Android classes - so it can be unit-tested directly (see RecipeMatcherTest).
 */
public class RecipeMatcher {

    /** Tolerance for floating-point rounding after unit conversions. */
    private static final double EPSILON = 1e-6;

    /** Pantry items grouped by normalised name, so duplicates ("egg" + "eggs") add up. */
    private final Map<String, List<PantryItem>> pantryByName = new HashMap<>();

    public RecipeMatcher(List<PantryItem> pantry) {
        for (PantryItem item : pantry) {
            if (item.getQuantity() <= 0) continue;
            String key = IngredientNormalizer.normalize(item.getName());
            List<PantryItem> group = pantryByName.get(key);
            if (group == null) {
                group = new ArrayList<>();
                pantryByName.put(key, group);
            }
            group.add(item);
        }
    }

    /** Checks every ingredient of one recipe against the pantry. */
    public MatchResult match(Recipe recipe) {
        List<MatchResult.IngredientCheck> checks = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            checks.add(check(ingredient));
        }
        return new MatchResult(recipe, checks);
    }

    /** Matches all recipes; also records how many expiring-soon items each one would use up. */
    public List<MatchResult> matchAll(List<Recipe> recipes, LocalDate today, int expiringWithinDays) {
        List<MatchResult> results = new ArrayList<>();
        for (Recipe recipe : recipes) {
            MatchResult result = match(recipe);
            result.setExpiringIngredientCount(countExpiringIngredients(recipe, today, expiringWithinDays));
            results.add(result);
        }
        return results;
    }

    private MatchResult.IngredientCheck check(RecipeIngredient ingredient) {
        List<PantryItem> candidates = pantryByName.get(IngredientNormalizer.normalize(ingredient.getName()));
        if (candidates == null || candidates.isEmpty()) {
            return new MatchResult.IngredientCheck(ingredient, MatchResult.Status.MISSING, 0);
        }

        double available = 0;
        boolean anyComparable = false;
        for (PantryItem item : candidates) {
            Double converted = UnitConverter.convert(
                    item.getQuantity(), item.getUnit(), ingredient.getUnit(), ingredient.getName());
            if (converted != null) {
                available += converted;
                anyComparable = true;
            }
        }

        if (!anyComparable) {
            return new MatchResult.IngredientCheck(ingredient, MatchResult.Status.UNIT_MISMATCH, 0);
        }
        MatchResult.Status status = available + EPSILON >= ingredient.getQuantity()
                ? MatchResult.Status.HAVE
                : MatchResult.Status.NOT_ENOUGH;
        return new MatchResult.IngredientCheck(ingredient, status, available);
    }

    private int countExpiringIngredients(Recipe recipe, LocalDate today, int days) {
        int count = 0;
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            List<PantryItem> candidates = pantryByName.get(IngredientNormalizer.normalize(ingredient.getName()));
            if (candidates == null) continue;
            for (PantryItem item : candidates) {
                if (item.isExpiringWithin(today, days)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /** Only recipes that pass the strict rule; recipes using up expiring food are listed first. */
    public static List<MatchResult> strictMatches(List<MatchResult> results) {
        List<MatchResult> strict = new ArrayList<>();
        for (MatchResult r : results) {
            if (r.isStrictMatch()) strict.add(r);
        }
        Collections.sort(strict, BY_EXPIRING_THEN_NAME);
        return strict;
    }

    /** Recipes missing exactly one ingredient - the optional, clearly separate "Almost There" list. */
    public static List<MatchResult> almostThere(List<MatchResult> results) {
        List<MatchResult> almost = new ArrayList<>();
        for (MatchResult r : results) {
            if (r.isAlmostThere()) almost.add(r);
        }
        Collections.sort(almost, BY_EXPIRING_THEN_NAME);
        return almost;
    }

    private static final Comparator<MatchResult> BY_EXPIRING_THEN_NAME = new Comparator<MatchResult>() {
        @Override
        public int compare(MatchResult a, MatchResult b) {
            int byExpiring = Integer.compare(b.getExpiringIngredientCount(), a.getExpiringIngredientCount());
            if (byExpiring != 0) return byExpiring;
            return a.getRecipe().getName().compareToIgnoreCase(b.getRecipe().getName());
        }
    };

    /** Removes expired items, so recipes are never suggested using food that has gone off. */
    public static List<PantryItem> withoutExpired(List<PantryItem> pantry, LocalDate today) {
        List<PantryItem> fresh = new ArrayList<>();
        for (PantryItem item : pantry) {
            if (!item.isExpired(today)) fresh.add(item);
        }
        return fresh;
    }

    /**
     * Works out how the pantry changes when the user cooks {@code recipe}: the new quantity of
     * every pantry item that gets used (0 or less means the item is used up). Items expiring
     * soonest are used first, to cut waste.
     *
     * @throws IllegalStateException if the recipe is not a strict match
     */
    public Map<Long, Double> planConsumption(Recipe recipe) {
        if (!match(recipe).isStrictMatch()) {
            throw new IllegalStateException("Cannot cook " + recipe.getName() + ": ingredients missing");
        }

        // Working copy of quantities, in case two recipe lines draw on the same pantry item
        Map<Long, Double> newQuantities = new LinkedHashMap<>();

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            List<PantryItem> candidates =
                    new ArrayList<>(pantryByName.get(IngredientNormalizer.normalize(ingredient.getName())));
            Collections.sort(candidates, SOONEST_EXPIRY_FIRST);

            double stillNeeded = ingredient.getQuantity();
            for (PantryItem item : candidates) {
                if (stillNeeded <= EPSILON) break;
                Double current = newQuantities.get(item.getId());
                double itemQty = current != null ? current : item.getQuantity();
                Double availableInRecipeUnit = UnitConverter.convert(
                        itemQty, item.getUnit(), ingredient.getUnit(), ingredient.getName());
                if (availableInRecipeUnit == null || availableInRecipeUnit <= EPSILON) continue;

                double take = Math.min(availableInRecipeUnit, stillNeeded);
                Double takeInItemUnit = UnitConverter.convert(
                        take, ingredient.getUnit(), item.getUnit(), ingredient.getName());
                double remaining = itemQty - takeInItemUnit;
                newQuantities.put(item.getId(), remaining < EPSILON ? 0 : remaining);
                stillNeeded -= take;
            }
            if (stillNeeded > EPSILON) {
                throw new IllegalStateException("Not enough " + ingredient.getName());
            }
        }
        return newQuantities;
    }

    private static final Comparator<PantryItem> SOONEST_EXPIRY_FIRST = new Comparator<PantryItem>() {
        @Override
        public int compare(PantryItem a, PantryItem b) {
            LocalDate ea = a.getExpiryLocalDate();
            LocalDate eb = b.getExpiryLocalDate();
            if (ea == null && eb == null) return 0;
            if (ea == null) return 1;   // items without an expiry date are used last
            if (eb == null) return -1;
            return ea.compareTo(eb);
        }
    };
}
