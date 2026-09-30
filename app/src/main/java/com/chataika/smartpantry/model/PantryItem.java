package com.chataika.smartpantry.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * One ingredient the user has at home. Maps 1:1 to a row in the pantry_items table.
 * Kept free of Android classes so the matching logic can be unit-tested on the JVM.
 */
public class PantryItem {

    private long id;
    private String name;
    private double quantity;
    private String unit;
    /** ISO-8601 date (yyyy-MM-dd) or null when the user did not set an expiry date. */
    private String expiryDate;

    public PantryItem(long id, String name, double quantity, String unit, String expiryDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    /** Constructor for a new item that has not been saved yet (id = 0). */
    public PantryItem(String name, double quantity, String unit, String expiryDate) {
        this(0, name, quantity, unit, expiryDate);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    /** Parsed expiry date, or null if none was set or the stored value is invalid. */
    public LocalDate getExpiryLocalDate() {
        if (expiryDate == null || expiryDate.isEmpty()) return null;
        try {
            return LocalDate.parse(expiryDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public boolean isExpired(LocalDate today) {
        LocalDate expiry = getExpiryLocalDate();
        return expiry != null && expiry.isBefore(today);
    }

    /** True when the item has not expired yet but will within {@code days} days. */
    public boolean isExpiringWithin(LocalDate today, int days) {
        LocalDate expiry = getExpiryLocalDate();
        return expiry != null && !expiry.isBefore(today) && !expiry.isAfter(today.plusDays(days));
    }
}
