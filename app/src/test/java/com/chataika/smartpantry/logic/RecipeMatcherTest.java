package com.chataika.smartpantry.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.chataika.smartpantry.data.SeedData;
import com.chataika.smartpantry.model.PantryItem;
import com.chataika.smartpantry.model.Recipe;
import com.chataika.smartpantry.model.RecipeIngredient;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Unit tests for the strict-matching rule (brief section 2.3). */
public class RecipeMatcherTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);

    /** A recipe needing 5 ingredients. */
    private final Recipe omelette = new Recipe(1, "Omelette", "", 10,
            Collections.singletonList("Cook it"),
            Arrays.asList(
                    new RecipeIngredient("egg", 3, "pcs"),
                    new RecipeIngredient("tomato", 1, "pcs"),
                    new RecipeIngredient("onion", 1, "pcs"),
                    new RecipeIngredient("oil", 1, "tbsp"),
                    new RecipeIngredient("salt", 0.5, "tsp")));

    private static PantryItem item(String name, double qty, String unit) {
        return new PantryItem(name, qty, unit, null);
    }

    private List<PantryItem> fullPantry() {
        return new ArrayList<>(Arrays.asList(
                item("egg", 6, "pcs"), item("tomato", 2, "pcs"), item("onion", 1, "pcs"),
                item("oil", 500, "ml"), item("salt", 100, "g")));
    }

    @Test
    public void allIngredientsPresent_isStrictMatch() {
        MatchResult result = new RecipeMatcher(fullPantry()).match(omelette);
        assertTrue(result.isStrictMatch());
        assertEquals(0, result.getMissingCount());
    }

    @Test
    public void fourOfFiveIngredients_isNotSuggested() {
        List<PantryItem> pantry = fullPantry();
        pantry.remove(2); // remove the onion
        MatchResult result = new RecipeMatcher(pantry).match(omelette);
        assertFalse(result.isStrictMatch());
        assertEquals(1, result.getMissingCount());
        assertTrue(RecipeMatcher.strictMatches(Collections.singletonList(result)).isEmpty());
        assertEquals(1, RecipeMatcher.almostThere(Collections.singletonList(result)).size());
    }

    @Test
    public void insufficientQuantity_isNotSuggested() {
        List<PantryItem> pantry = fullPantry();
        pantry.set(0, item("egg", 2, "pcs")); // recipe needs 3
        MatchResult result = new RecipeMatcher(pantry).match(omelette);
        assertFalse(result.isStrictMatch());
        assertEquals(MatchResult.Status.NOT_ENOUGH, result.getChecks().get(0).getStatus());
    }

    @Test
    public void duplicatePantryEntries_areAddedTogether() {
        List<PantryItem> pantry = fullPantry();
        pantry.set(0, item("Egg", 2, "pcs"));
        pantry.add(item("eggs", 1, "pcs")); // 2 + 1 = 3 eggs
        assertTrue(new RecipeMatcher(pantry).match(omelette).isStrictMatch());
    }

    @Test
    public void pluralAndCaseDifferences_stillMatch() {
        List<PantryItem> pantry = fullPantry();
        pantry.set(0, item("  EGGS ", 6, "pieces"));
        pantry.set(1, item("Tomatoes", 2, "pcs"));
        pantry.set(2, item("Fresh Red Onions", 1, "pcs"));
        assertTrue(new RecipeMatcher(pantry).match(omelette).isStrictMatch());
    }

    @Test
    public void normalizer_handlesCommonPluralsAndSynonyms() {
        assertEquals("tomato", IngredientNormalizer.normalize("Tomatoes"));
        assertEquals("potato", IngredientNormalizer.normalize("potatoes"));
        assertEquals("berry", IngredientNormalizer.normalize("berries"));
        assertEquals("spring onion", IngredientNormalizer.normalize("Scallions"));
        assertEquals("minced beef", IngredientNormalizer.normalize("beef mince"));
        assertEquals("hummus", IngredientNormalizer.normalize("hummus"));
        assertEquals("egg", IngredientNormalizer.normalize("Large free-range eggs"));
    }

    @Test
    public void unitConversion_withinSameDimension() {
        assertEquals(500.0, UnitConverter.convert(0.5, "kg", "g", "pasta"), 1e-9);
        assertEquals(250.0, UnitConverter.convert(1, "cup", "ml", "milk"), 1e-9);
        assertEquals(3.0, UnitConverter.convert(1, "tbsp", "tsp", "salt"), 1e-9);
    }

    @Test
    public void unitConversion_massToVolumeOnlyWithKnownDensity() {
        // 1 cup of flour ~ 132.5 g
        assertEquals(132.5, UnitConverter.convert(1, "cup", "g", "flour"), 1e-9);
        // unknown density -> not comparable
        assertNull(UnitConverter.convert(1, "cup", "g", "spinach"));
        // pcs cannot be compared with grams
        assertNull(UnitConverter.convert(1, "pcs", "g", "tomato"));
        // cloves and pieces are different counts
        assertNull(UnitConverter.convert(1, "pcs", "clove", "garlic"));
        assertNotNull(UnitConverter.convert(1, "tin", "pcs", "tuna"));
    }

    @Test
    public void kilogramsInPantry_satisfyCupsInRecipe() {
        Recipe pancakes = new Recipe(2, "Pancakes", "", 20, Collections.singletonList("Cook"),
                Collections.singletonList(new RecipeIngredient("flour", 2, "cup")));
        assertTrue(new RecipeMatcher(Collections.singletonList(item("Cake flour", 1, "kg")))
                .match(pancakes).isStrictMatch());
        assertFalse(new RecipeMatcher(Collections.singletonList(item("flour", 100, "g")))
                .match(pancakes).isStrictMatch());
    }

    @Test
    public void incomparableUnits_areNotAMatch() {
        List<PantryItem> pantry = fullPantry();
        pantry.set(1, item("tomato", 500, "g")); // recipe wants 1 pcs
        MatchResult result = new RecipeMatcher(pantry).match(omelette);
        assertFalse(result.isStrictMatch());
        assertEquals(MatchResult.Status.UNIT_MISMATCH, result.getChecks().get(1).getStatus());
    }

    @Test
    public void expiredItems_canBeExcluded() {
        List<PantryItem> pantry = fullPantry();
        pantry.set(0, new PantryItem("egg", 6, "pcs", TODAY.minusDays(1).toString()));
        List<PantryItem> fresh = RecipeMatcher.withoutExpired(pantry, TODAY);
        assertFalse(new RecipeMatcher(fresh).match(omelette).isStrictMatch());
    }

    @Test
    public void emptyPantry_matchesNothing() {
        RecipeMatcher matcher = new RecipeMatcher(Collections.<PantryItem>emptyList());
        List<MatchResult> results = matcher.matchAll(SeedData.recipes(), TODAY, 3);
        assertTrue(RecipeMatcher.strictMatches(results).isEmpty());
    }

    @Test
    public void planConsumption_usesSoonestExpiringFirst() {
        PantryItem oldEggs = new PantryItem(1, "egg", 2, "pcs", TODAY.plusDays(1).toString());
        PantryItem newEggs = new PantryItem(2, "egg", 6, "pcs", TODAY.plusDays(20).toString());
        Recipe r = new Recipe(3, "Eggs", "", 5, Collections.singletonList("Cook"),
                Collections.singletonList(new RecipeIngredient("eggs", 3, "pcs")));

        Map<Long, Double> plan = new RecipeMatcher(Arrays.asList(newEggs, oldEggs)).planConsumption(r);
        assertEquals(0.0, plan.get(1L), 1e-9);  // both old eggs used up
        assertEquals(5.0, plan.get(2L), 1e-9);  // one new egg used
    }

    @Test
    public void planConsumption_convertsBackToPantryUnit() {
        PantryItem milk = new PantryItem(7, "milk", 1, "l", null);
        Recipe r = new Recipe(4, "Milky", "", 5, Collections.singletonList("Pour"),
                Collections.singletonList(new RecipeIngredient("milk", 250, "ml")));
        Map<Long, Double> plan = new RecipeMatcher(Collections.singletonList(milk)).planConsumption(r);
        assertEquals(0.75, plan.get(7L), 1e-9);
    }

    @Test(expected = IllegalStateException.class)
    public void planConsumption_rejectsNonMatchingRecipe() {
        new RecipeMatcher(Collections.<PantryItem>emptyList()).planConsumption(omelette);
    }

    @Test
    public void seedData_hasEnoughRecipes_andEveryRecipeIsCookable() {
        List<Recipe> recipes = SeedData.recipes();
        assertTrue(recipes.size() >= 15);

        // Build a pantry containing a generous amount of every ingredient, in the recipe's unit.
        // Every recipe must then be a strict match - proves no seed recipe is un-matchable.
        List<PantryItem> pantry = new ArrayList<>();
        for (Recipe recipe : recipes) {
            for (RecipeIngredient ing : recipe.getIngredients()) {
                pantry.add(item(ing.getName(), ing.getQuantity() * 10, ing.getUnit()));
            }
        }
        List<MatchResult> results = new RecipeMatcher(pantry).matchAll(recipes, TODAY, 3);
        assertEquals(recipes.size(), RecipeMatcher.strictMatches(results).size());
    }

    @Test
    public void samplePantry_givesStrictAndAlmostThereResults() {
        List<MatchResult> results = new RecipeMatcher(SeedData.samplePantry(TODAY))
                .matchAll(SeedData.recipes(), TODAY, 3);
        List<MatchResult> strict = RecipeMatcher.strictMatches(results);
        assertFalse(strict.isEmpty());
        for (MatchResult r : strict) assertEquals(0, r.getMissingCount());
        for (MatchResult r : RecipeMatcher.almostThere(results)) assertEquals(1, r.getMissingCount());
    }
}
