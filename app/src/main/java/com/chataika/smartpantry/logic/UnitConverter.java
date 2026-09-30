package com.chataika.smartpantry.logic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Converts quantities between units so the pantry and recipes do not have to use
 * identical units, e.g. a pantry holding "1 kg flour" satisfies a recipe needing "2 cup flour".
 *
 * Units are grouped by dimension (mass, volume, count). Conversions inside the same
 * dimension use fixed factors. Mass <-> volume is only allowed for ingredients whose
 * density we know. Anything we cannot convert returns null, and the matcher then treats
 * the ingredient as missing - wrongly suggesting a recipe is worse than missing one.
 */
public final class UnitConverter {

    enum Dimension { MASS, VOLUME, COUNT }

    private static final class UnitInfo {
        final Dimension dimension;
        final double toBase; // grams for MASS, millilitres for VOLUME, 1 for COUNT

        UnitInfo(Dimension dimension, double toBase) {
            this.dimension = dimension;
            this.toBase = toBase;
        }
    }

    /** Units the app offers in its unit picker (canonical spellings). */
    public static final String[] PICKER_UNITS = {
            "pcs", "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "slice", "clove", "tin"
    };

    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, UnitInfo> UNITS = new HashMap<>();
    /** Approximate density in grams per millilitre, keyed by normalised ingredient name. */
    private static final Map<String, Double> DENSITY = new HashMap<>();

    static {
        // Generic "count" spellings. A tin/can is counted like any other piece.
        for (String a : new String[]{"", "pc", "piece", "pieces", "whole", "each", "unit", "units",
                "item", "items", "tin", "tins", "can", "cans"}) {
            ALIASES.put(a, "pcs");
        }
        ALIASES.put("slices", "slice");
        ALIASES.put("cloves", "clove");
        ALIASES.put("gram", "g");
        ALIASES.put("grams", "g");
        ALIASES.put("gr", "g");
        ALIASES.put("kgs", "kg");
        ALIASES.put("kilogram", "kg");
        ALIASES.put("kilograms", "kg");
        ALIASES.put("litre", "l");
        ALIASES.put("litres", "l");
        ALIASES.put("liter", "l");
        ALIASES.put("liters", "l");
        ALIASES.put("lt", "l");
        ALIASES.put("millilitre", "ml");
        ALIASES.put("millilitres", "ml");
        ALIASES.put("milliliter", "ml");
        ALIASES.put("milliliters", "ml");
        ALIASES.put("teaspoon", "tsp");
        ALIASES.put("teaspoons", "tsp");
        ALIASES.put("tablespoon", "tbsp");
        ALIASES.put("tablespoons", "tbsp");
        ALIASES.put("cups", "cup");

        UNITS.put("g", new UnitInfo(Dimension.MASS, 1));
        UNITS.put("kg", new UnitInfo(Dimension.MASS, 1000));
        UNITS.put("ml", new UnitInfo(Dimension.VOLUME, 1));
        UNITS.put("l", new UnitInfo(Dimension.VOLUME, 1000));
        UNITS.put("tsp", new UnitInfo(Dimension.VOLUME, 5));
        UNITS.put("tbsp", new UnitInfo(Dimension.VOLUME, 15));
        UNITS.put("cup", new UnitInfo(Dimension.VOLUME, 250));
        UNITS.put("pcs", new UnitInfo(Dimension.COUNT, 1));
        UNITS.put("slice", new UnitInfo(Dimension.COUNT, 1));
        UNITS.put("clove", new UnitInfo(Dimension.COUNT, 1));

        DENSITY.put("flour", 0.53);
        DENSITY.put("sugar", 0.85);
        DENSITY.put("salt", 1.2);
        DENSITY.put("rice", 0.85);
        DENSITY.put("oat", 0.41);
        DENSITY.put("lentil", 0.8);
        DENSITY.put("butter", 0.96);
        DENSITY.put("oil", 0.92);
        DENSITY.put("olive oil", 0.92);
        DENSITY.put("milk", 1.03);
        DENSITY.put("water", 1.0);
        DENSITY.put("soy sauce", 1.1);
        DENSITY.put("mayonnaise", 0.95);
        DENSITY.put("curry powder", 0.45);
        DENSITY.put("baking powder", 0.9);
        DENSITY.put("cinnamon", 0.55);
        DENSITY.put("honey", 1.42);
    }

    private UnitConverter() { }

    /** Canonical spelling of a unit: "Grams" -> "g", "tins" -> "pcs", "" -> "pcs". */
    public static String normalizeUnit(String unit) {
        String u = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        if (u.endsWith(".")) u = u.substring(0, u.length() - 1);
        String alias = ALIASES.get(u);
        return alias != null ? alias : u;
    }

    /**
     * Converts {@code amount} from one unit to another for the given ingredient.
     *
     * @return the converted amount, or null when the units cannot be compared
     */
    public static Double convert(double amount, String fromUnit, String toUnit, String ingredientName) {
        String from = normalizeUnit(fromUnit);
        String to = normalizeUnit(toUnit);
        if (from.equals(to)) return amount;

        UnitInfo f = UNITS.get(from);
        UnitInfo t = UNITS.get(to);
        if (f == null || t == null) return null;

        if (f.dimension == t.dimension) {
            // Different count units (pcs vs slice vs clove) are not interchangeable
            if (f.dimension == Dimension.COUNT) return null;
            return amount * f.toBase / t.toBase;
        }

        if (f.dimension == Dimension.COUNT || t.dimension == Dimension.COUNT) return null;

        // Mass <-> volume needs a known density for this ingredient
        Double density = DENSITY.get(IngredientNormalizer.normalize(ingredientName));
        if (density == null) return null;
        if (f.dimension == Dimension.MASS) {
            double millilitres = amount * f.toBase / density;
            return millilitres / t.toBase;
        } else {
            double grams = amount * f.toBase * density;
            return grams / t.toBase;
        }
    }
}
