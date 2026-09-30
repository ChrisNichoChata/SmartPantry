package com.chataika.smartpantry.logic;

import java.util.Locale;

/** Formatting helpers for displaying quantities without noisy decimals ("2" not "2.0"). */
public final class Quantities {

    private Quantities() { }

    public static String format(double quantity) {
        if (Math.abs(quantity - Math.rint(quantity)) < 1e-9) {
            return String.valueOf((long) Math.rint(quantity));
        }
        String s = String.format(Locale.US, "%.2f", quantity);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    /** Quantity with its unit, e.g. "1.5 kg" or "3 pcs". */
    public static String format(double quantity, String unit) {
        return unit == null || unit.trim().isEmpty() ? format(quantity) : format(quantity) + " " + unit.trim();
    }
}
