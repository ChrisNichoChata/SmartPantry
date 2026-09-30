package com.chataika.smartpantry.logic;

import java.util.regex.Pattern;

/**
 * Validation rules for the Add/Edit Ingredient form. Returns error codes rather than
 * messages so the rules stay testable and the Activity picks the string resource.
 */
public final class IngredientValidator {

    public enum NameError { NONE, REQUIRED, LENGTH, INVALID_CHARS }
    public enum QuantityError { NONE, REQUIRED, INVALID, TOO_LARGE }

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 40;
    public static final double MAX_QUANTITY = 100_000;

    /** Starts with a letter; then letters, spaces, hyphens or apostrophes ("crème fraîche", "hot-dog roll"). */
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L} '\\-]*$");

    private IngredientValidator() { }

    public static NameError validateName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) return NameError.REQUIRED;
        if (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH) return NameError.LENGTH;
        if (!NAME_PATTERN.matcher(name).matches()) return NameError.INVALID_CHARS;
        return NameError.NONE;
    }

    public static QuantityError validateQuantity(String raw) {
        if (raw == null || raw.trim().isEmpty()) return QuantityError.REQUIRED;
        Double value = parseQuantity(raw);
        if (value == null || value <= 0) return QuantityError.INVALID;
        if (value > MAX_QUANTITY) return QuantityError.TOO_LARGE;
        return QuantityError.NONE;
    }

    /** Parses "1.5" or "1,5" (comma decimal separator is common in South Africa). Null if invalid. */
    public static Double parseQuantity(String raw) {
        if (raw == null) return null;
        try {
            double value = Double.parseDouble(raw.trim().replace(',', '.'));
            return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
