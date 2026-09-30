package com.chataika.smartpantry.data;

import android.content.Context;
import android.content.SharedPreferences;

/** User preferences from the Settings screen, stored in SharedPreferences. */
public class AppSettings {

    private static final String PREFS_NAME = "smart_pantry_settings";
    private static final String KEY_EXPIRY_ALERTS = "expiry_alerts";
    private static final String KEY_EXPIRY_DAYS = "expiry_days";
    private static final String KEY_IGNORE_EXPIRED = "ignore_expired";
    private static final String KEY_SHOW_ALMOST_THERE = "show_almost_there";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Show a warning banner and highlight items that expire soon. */
    public boolean isExpiryAlertsEnabled() { return prefs.getBoolean(KEY_EXPIRY_ALERTS, true); }
    public void setExpiryAlertsEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_EXPIRY_ALERTS, enabled).apply(); }

    /** How many days ahead counts as "expiring soon". */
    public int getExpiryWarningDays() { return prefs.getInt(KEY_EXPIRY_DAYS, 3); }
    public void setExpiryWarningDays(int days) { prefs.edit().putInt(KEY_EXPIRY_DAYS, days).apply(); }

    /** Leave expired items out of recipe matching. */
    public boolean isIgnoreExpired() { return prefs.getBoolean(KEY_IGNORE_EXPIRED, true); }
    public void setIgnoreExpired(boolean ignore) { prefs.edit().putBoolean(KEY_IGNORE_EXPIRED, ignore).apply(); }

    /** Show the separate "Almost There" (missing one ingredient) list. */
    public boolean isShowAlmostThere() { return prefs.getBoolean(KEY_SHOW_ALMOST_THERE, true); }
    public void setShowAlmostThere(boolean show) { prefs.edit().putBoolean(KEY_SHOW_ALMOST_THERE, show).apply(); }
}
