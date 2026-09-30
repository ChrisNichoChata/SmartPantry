package com.chataika.smartpantry.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns a free-text ingredient name into a canonical key so that trivial differences
 * do not break matching: "Tomatoes", " fresh tomato " and "TOMATO" all become "tomato".
 *
 * Steps: lower-case, strip punctuation, drop descriptive words (fresh, large, ...),
 * singularise each word, then map known synonyms ("scallion" -> "spring onion").
 */
public final class IngredientNormalizer {

    /** Adjectives that describe an ingredient but do not change what it is. */
    private static final Set<String> DESCRIPTORS = new HashSet<>(Arrays.asList(
            "fresh", "large", "medium", "small", "chopped", "diced", "sliced",
            "ripe", "organic", "free", "range", "grated", "peeled"));

    /** Plurals that the simple suffix rules below would get wrong. */
    private static final Map<String, String> IRREGULAR_PLURALS = new HashMap<>();

    /** Different names for the same ingredient, mapped to one canonical name. */
    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        IRREGULAR_PLURALS.put("leaves", "leaf");
        IRREGULAR_PLURALS.put("loaves", "loaf");
        IRREGULAR_PLURALS.put("halves", "half");

        SYNONYMS.put("scallion", "spring onion");
        SYNONYMS.put("green onion", "spring onion");
        SYNONYMS.put("courgette", "zucchini");
        SYNONYMS.put("aubergine", "eggplant");
        SYNONYMS.put("cilantro", "coriander");
        SYNONYMS.put("capsicum", "bell pepper");
        SYNONYMS.put("green pepper", "bell pepper");
        SYNONYMS.put("red pepper", "bell pepper");
        SYNONYMS.put("yellow pepper", "bell pepper");
        SYNONYMS.put("mince", "minced beef");
        SYNONYMS.put("beef mince", "minced beef");
        SYNONYMS.put("ground beef", "minced beef");
        SYNONYMS.put("mealie meal", "maize meal");
        SYNONYMS.put("maizemeal", "maize meal");
        SYNONYMS.put("plain flour", "flour");
        SYNONYMS.put("cake flour", "flour");
        SYNONYMS.put("all purpose flour", "flour");
        SYNONYMS.put("white sugar", "sugar");
        SYNONYMS.put("brown sugar", "sugar");
        SYNONYMS.put("castor sugar", "sugar");
        SYNONYMS.put("chicken breast", "chicken");
        SYNONYMS.put("chicken thigh", "chicken");
        SYNONYMS.put("chicken fillet", "chicken");
        SYNONYMS.put("full cream milk", "milk");
        SYNONYMS.put("low fat milk", "milk");
        SYNONYMS.put("red onion", "onion");
        SYNONYMS.put("white onion", "onion");
        SYNONYMS.put("brown onion", "onion");
        SYNONYMS.put("spaghetti", "pasta");
        SYNONYMS.put("penne", "pasta");
        SYNONYMS.put("macaroni", "pasta");
        SYNONYMS.put("fusilli", "pasta");
        SYNONYMS.put("cheddar", "cheese");
        SYNONYMS.put("cheddar cheese", "cheese");
        SYNONYMS.put("feta", "feta cheese");
        SYNONYMS.put("vegetable oil", "oil");
        SYNONYMS.put("sunflower oil", "oil");
        SYNONYMS.put("cooking oil", "oil");
        SYNONYMS.put("garlic clove", "garlic");
        SYNONYMS.put("mayo", "mayonnaise");
        SYNONYMS.put("porridge oat", "oat");
        SYNONYMS.put("rolled oat", "oat");
        SYNONYMS.put("white rice", "rice");
        SYNONYMS.put("basmati rice", "rice");
        SYNONYMS.put("wrap", "tortilla");
        SYNONYMS.put("stock", "stock cube");
        SYNONYMS.put("bouillon cube", "stock cube");
        SYNONYMS.put("corn", "sweetcorn");
        SYNONYMS.put("sweet corn", "sweetcorn");
    }

    private IngredientNormalizer() { }

    /** Returns the canonical matching key for an ingredient name ("" for null/blank input). */
    public static String normalize(String name) {
        if (name == null) return "";
        String cleaned = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").trim();
        if (cleaned.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (DESCRIPTORS.contains(word)) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(singularize(word));
        }
        // If every word was a descriptor (e.g. "fresh"), fall back to the cleaned text
        String key = sb.length() > 0 ? sb.toString() : cleaned;

        String synonym = SYNONYMS.get(key);
        return synonym != null ? synonym : key;
    }

    /** Simple English singularisation - good enough for ingredient names. */
    static String singularize(String word) {
        if (word.length() <= 3) return word;              // "gas", "pea" etc. stay as-is
        String irregular = IRREGULAR_PLURALS.get(word);
        if (irregular != null) return irregular;

        if (word.endsWith("ies")) {                        // berries -> berry
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("oes")) {                        // tomatoes -> tomato
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("ches") || word.endsWith("shes")
                || word.endsWith("sses") || word.endsWith("xes")) {   // radishes -> radish
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss")
                && !word.endsWith("us") && !word.endsWith("is")) {    // eggs -> egg, but not hummus
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
