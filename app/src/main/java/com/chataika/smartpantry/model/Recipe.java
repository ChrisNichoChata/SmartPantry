package com.chataika.smartpantry.model;

import java.util.ArrayList;
import java.util.List;

/** A recipe from the pre-seeded collection, with its required ingredients and method. */
public class Recipe {

    private final long id;
    private final String name;
    private final String description;
    private final int prepMinutes;
    private final List<String> steps;
    private final List<RecipeIngredient> ingredients;

    public Recipe(long id, String name, String description, int prepMinutes,
                  List<String> steps, List<RecipeIngredient> ingredients) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prepMinutes = prepMinutes;
        this.steps = new ArrayList<>(steps);
        this.ingredients = new ArrayList<>(ingredients);
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrepMinutes() { return prepMinutes; }
    public List<String> getSteps() { return steps; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
}
