package com.chataika.smartpantry.data;

import com.chataika.smartpantry.model.PantryItem;
import com.chataika.smartpantry.model.Recipe;
import com.chataika.smartpantry.model.RecipeIngredient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The built-in recipe collection (inserted into SQLite on first run) and an optional
 * sample pantry used for demonstrations. Plain Java so it can also be used in unit tests.
 */
public final class SeedData {

    private SeedData() { }

    public static List<Recipe> recipes() {
        List<Recipe> r = new ArrayList<>();

        r.add(recipe("Scrambled Eggs on Toast", "Soft, creamy eggs on buttered toast.", 10,
                steps("Whisk the eggs with the milk and salt.",
                        "Melt half the butter in a pan over low heat.",
                        "Add the eggs and stir gently until just set.",
                        "Toast the bread, butter it and top with the eggs."),
                ing("egg", 3, "pcs"), ing("bread", 2, "slice"), ing("butter", 10, "g"),
                ing("milk", 30, "ml"), ing("salt", 0.5, "tsp")));

        r.add(recipe("Tomato & Onion Omelette", "A quick omelette that uses up a soft tomato.", 12,
                steps("Dice the tomato and onion.",
                        "Fry the onion in the oil until soft, then add the tomato for 2 minutes.",
                        "Beat the eggs with the salt and pour over the vegetables.",
                        "Cook until set, fold in half and serve."),
                ing("egg", 3, "pcs"), ing("tomato", 1, "pcs"), ing("onion", 1, "pcs"),
                ing("oil", 1, "tbsp"), ing("salt", 0.5, "tsp")));

        r.add(recipe("French Toast", "Great for bread that is starting to go stale.", 15,
                steps("Whisk the eggs, milk and sugar in a shallow dish.",
                        "Soak each slice of bread for a few seconds on both sides.",
                        "Fry in butter until golden on both sides."),
                ing("bread", 4, "slice"), ing("egg", 2, "pcs"), ing("milk", 100, "ml"),
                ing("sugar", 1, "tbsp"), ing("butter", 20, "g")));

        r.add(recipe("Pancakes", "Classic thin pancakes.", 25,
                steps("Whisk the flour, sugar, egg and milk into a smooth batter.",
                        "Rest the batter for 10 minutes.",
                        "Melt a little butter in a pan and pour in a thin layer of batter.",
                        "Cook for 1 minute per side and repeat."),
                ing("flour", 1, "cup"), ing("egg", 1, "pcs"), ing("milk", 250, "ml"),
                ing("sugar", 2, "tbsp"), ing("butter", 15, "g")));

        r.add(recipe("Egg Fried Rice", "Best made with leftover cooked rice.", 20,
                steps("Cook the rice if it is not already cooked, then let it cool.",
                        "Fry the diced onion and grated carrot in the oil.",
                        "Push the vegetables aside, scramble the eggs in the pan.",
                        "Add the rice and soy sauce and stir-fry for 3 minutes."),
                ing("rice", 1, "cup"), ing("egg", 2, "pcs"), ing("onion", 1, "pcs"),
                ing("carrot", 1, "pcs"), ing("soy sauce", 2, "tbsp"), ing("oil", 2, "tbsp")));

        r.add(recipe("Tomato Pasta", "A simple fresh tomato sauce.", 25,
                steps("Boil the pasta in salted water until al dente.",
                        "Fry the chopped onion and garlic in the oil.",
                        "Add the chopped tomatoes and salt and simmer for 10 minutes.",
                        "Toss the drained pasta through the sauce."),
                ing("pasta", 250, "g"), ing("tomato", 4, "pcs"), ing("onion", 1, "pcs"),
                ing("garlic", 2, "clove"), ing("oil", 2, "tbsp"), ing("salt", 1, "tsp")));

        r.add(recipe("Pap and Chakalaka", "South African favourite: stiff pap with spicy relish.", 40,
                steps("Bring 750 ml water to the boil and stir in the maize meal.",
                        "Cover and steam on low heat for 20 minutes, stirring now and then.",
                        "For the chakalaka, fry the onion and curry powder in the oil.",
                        "Add grated carrots and chopped tomatoes and cook for 10 minutes.",
                        "Stir in the baked beans, heat through and serve with the pap."),
                ing("maize meal", 250, "g"), ing("onion", 1, "pcs"), ing("tomato", 2, "pcs"),
                ing("carrot", 2, "pcs"), ing("baked beans", 1, "tin"),
                ing("curry powder", 1, "tbsp"), ing("oil", 2, "tbsp")));

        r.add(recipe("Creamy Potato Soup", "Filling soup for potatoes that are sprouting.", 35,
                steps("Peel and dice the potatoes and onion.",
                        "Soften the onion in the butter.",
                        "Add the potatoes, the stock cube and 750 ml water; simmer 20 minutes.",
                        "Blend, stir in the milk and season with salt."),
                ing("potato", 4, "pcs"), ing("onion", 1, "pcs"), ing("butter", 30, "g"),
                ing("milk", 250, "ml"), ing("stock cube", 1, "pcs"), ing("salt", 1, "tsp")));

        r.add(recipe("Vegetable Stir-Fry", "Clears out the vegetable drawer in minutes.", 20,
                steps("Slice all the vegetables thinly.",
                        "Heat the oil until very hot and fry the garlic for 30 seconds.",
                        "Add the vegetables and stir-fry for 5 minutes.",
                        "Add the soy sauce, toss and serve."),
                ing("carrot", 2, "pcs"), ing("bell pepper", 1, "pcs"), ing("onion", 1, "pcs"),
                ing("cabbage", 200, "g"), ing("garlic", 2, "clove"),
                ing("soy sauce", 2, "tbsp"), ing("oil", 2, "tbsp")));

        r.add(recipe("Chicken Curry with Rice", "Mild, family-friendly curry.", 45,
                steps("Cook the rice.",
                        "Brown the chicken pieces in the oil and set aside.",
                        "Fry the onion and garlic, add the curry powder for 1 minute.",
                        "Add the tomatoes and chicken and simmer for 20 minutes.",
                        "Serve over the rice."),
                ing("chicken", 500, "g"), ing("onion", 1, "pcs"), ing("tomato", 2, "pcs"),
                ing("garlic", 3, "clove"), ing("curry powder", 2, "tbsp"),
                ing("oil", 2, "tbsp"), ing("rice", 1, "cup")));

        r.add(recipe("Tuna Pasta Salad", "Cold lunch that keeps well in the fridge.", 20,
                steps("Cook the pasta, drain and rinse under cold water.",
                        "Dice the cucumber and onion.",
                        "Mix the pasta, tuna, vegetables and mayonnaise."),
                ing("pasta", 200, "g"), ing("tuna", 1, "tin"), ing("mayonnaise", 3, "tbsp"),
                ing("cucumber", 1, "pcs"), ing("onion", 0.5, "pcs")));

        r.add(recipe("Banana Bread", "The best use for over-ripe bananas.", 70,
                steps("Heat the oven to 180 °C and grease a loaf tin.",
                        "Mash the bananas, then beat in the melted butter, sugar and eggs.",
                        "Fold in the flour and baking powder.",
                        "Bake for 50-60 minutes until a skewer comes out clean."),
                ing("banana", 3, "pcs"), ing("flour", 2, "cup"), ing("sugar", 0.5, "cup"),
                ing("egg", 2, "pcs"), ing("butter", 100, "g"), ing("baking powder", 2, "tsp")));

        r.add(recipe("Bean & Cheese Quesadillas", "Crispy tortillas with a melty filling.", 15,
                steps("Fry the diced onion until soft and mix with the beans.",
                        "Spread the mixture over one half of each tortilla and add the cheese.",
                        "Fold and toast in a dry pan until crisp on both sides."),
                ing("tortilla", 2, "pcs"), ing("baked beans", 1, "tin"),
                ing("cheese", 100, "g"), ing("onion", 1, "pcs")));

        r.add(recipe("Greek Salad", "Fresh salad for tomatoes and cucumber on their last legs.", 10,
                steps("Chop the cucumber and tomatoes into chunks.",
                        "Slice the onion thinly.",
                        "Top with crumbled feta and drizzle with olive oil."),
                ing("cucumber", 1, "pcs"), ing("tomato", 3, "pcs"), ing("onion", 1, "pcs"),
                ing("feta cheese", 100, "g"), ing("olive oil", 2, "tbsp")));

        r.add(recipe("Cottage Pie", "Savoury mince under a mashed potato crust.", 60,
                steps("Boil the potatoes, then mash with the butter and milk.",
                        "Brown the mince with the diced onion and grated carrots.",
                        "Crumble in the stock cube with 150 ml water and simmer 15 minutes.",
                        "Top the mince with the mash and bake at 200 °C for 20 minutes."),
                ing("minced beef", 500, "g"), ing("potato", 5, "pcs"), ing("onion", 1, "pcs"),
                ing("carrot", 2, "pcs"), ing("butter", 30, "g"), ing("milk", 100, "ml"),
                ing("stock cube", 1, "pcs")));

        r.add(recipe("Egg Fried Noodles", "Ten-minute noodles.", 10,
                steps("Cook the noodles and drain.",
                        "Scramble the eggs in the oil.",
                        "Add the noodles, sliced spring onions and soy sauce; toss well."),
                ing("noodles", 200, "g"), ing("egg", 2, "pcs"), ing("spring onion", 2, "pcs"),
                ing("soy sauce", 2, "tbsp"), ing("oil", 1, "tbsp")));

        r.add(recipe("Cheese & Tomato Toastie", "Toasted sandwich for the last slices of bread.", 8,
                steps("Butter the outside of both slices of bread.",
                        "Fill with the cheese and sliced tomato.",
                        "Toast in a pan until golden and the cheese has melted."),
                ing("bread", 2, "slice"), ing("cheese", 60, "g"),
                ing("butter", 10, "g"), ing("tomato", 1, "pcs")));

        r.add(recipe("Banana Oat Porridge", "Warm breakfast, naturally sweetened.", 10,
                steps("Simmer the oats in the milk for 5 minutes, stirring.",
                        "Stir in the sugar.",
                        "Top with the sliced banana."),
                ing("oats", 1, "cup"), ing("milk", 500, "ml"),
                ing("sugar", 1, "tbsp"), ing("banana", 1, "pcs")));

        r.add(recipe("Spinach & Feta Frittata", "Uses up wilting spinach.", 25,
                steps("Fry the onion in the oil in an oven-proof pan.",
                        "Add the spinach and cook until wilted.",
                        "Pour over the beaten eggs and crumble over the feta.",
                        "Cook on low heat for 5 minutes, then grill until set."),
                ing("spinach", 150, "g"), ing("egg", 5, "pcs"), ing("feta cheese", 80, "g"),
                ing("onion", 1, "pcs"), ing("oil", 1, "tbsp")));

        r.add(recipe("Lentil Soup", "Cheap, filling and freezer-friendly.", 40,
                steps("Fry the onion, carrots and garlic in the oil for 5 minutes.",
                        "Add the lentils, chopped tomatoes, stock cube and 1 litre water.",
                        "Simmer for 25 minutes until the lentils are soft."),
                ing("lentils", 200, "g"), ing("onion", 1, "pcs"), ing("carrot", 2, "pcs"),
                ing("garlic", 2, "clove"), ing("tomato", 2, "pcs"),
                ing("stock cube", 1, "pcs"), ing("oil", 1, "tbsp")));

        r.add(recipe("Garlic Butter Rice", "A simple side dish.", 20,
                steps("Cook the rice.",
                        "Melt the butter and gently fry the crushed garlic.",
                        "Stir the garlic butter and salt through the rice."),
                ing("rice", 1, "cup"), ing("butter", 30, "g"),
                ing("garlic", 3, "clove"), ing("salt", 0.5, "tsp")));

        return r;
    }

    /**
     * A sample pantry for demonstrations. It fully covers several recipes (e.g. French Toast,
     * Garlic Butter Rice) and leaves others one ingredient short, to show the strict rule.
     */
    public static List<PantryItem> samplePantry(LocalDate today) {
        return Arrays.asList(
                new PantryItem("Eggs", 6, "pcs", today.plusDays(10).toString()),
                new PantryItem("Bread", 8, "slice", today.plusDays(2).toString()),
                new PantryItem("Milk", 1, "l", today.plusDays(4).toString()),
                new PantryItem("Butter", 250, "g", null),
                new PantryItem("Sugar", 1, "kg", null),
                new PantryItem("Salt", 500, "g", null),
                new PantryItem("Rice", 1, "kg", null),
                new PantryItem("Garlic", 6, "clove", null),
                new PantryItem("Tomatoes", 3, "pcs", today.plusDays(1).toString()),
                new PantryItem("Onion", 2, "pcs", null),
                new PantryItem("Cooking oil", 750, "ml", null),
                new PantryItem("Cheddar cheese", 200, "g", today.plusDays(12).toString()));
    }

    private static Recipe recipe(String name, String description, int minutes,
                                 List<String> steps, RecipeIngredient... ingredients) {
        return new Recipe(0, name, description, minutes, steps, Arrays.asList(ingredients));
    }

    private static List<String> steps(String... steps) {
        return Arrays.asList(steps);
    }

    private static RecipeIngredient ing(String name, double quantity, String unit) {
        return new RecipeIngredient(name, quantity, unit);
    }
}
