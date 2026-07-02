package com.budgettracker.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AppPrefs - replaces SessionManager for a no-login single-user app.
 * Stores app-wide preferences: budget limit, user display name, currency.
 */
public class AppPrefs {

    private static final String PREF_NAME    = "BudgetAppPrefs";
    private static final String KEY_BUDGET   = "monthlyBudgetLimit";
    private static final String KEY_NAME     = "displayName";
    private static final String KEY_CURRENCY = "currencySymbol";

    /** Single-user app — always use userId = 1 */
    public static final int USER_ID = 1;

    public static int getUserId() {
        return USER_ID;
    }

    // ── Budget limit ──────────────────────────────────────────────────────────

    public static double getBudgetLimit(Context context) {
        return prefs(context).getFloat(KEY_BUDGET, 0f);
    }

    public static void setBudgetLimit(Context context, double limit) {
        prefs(context).edit().putFloat(KEY_BUDGET, (float) limit).apply();
    }

    // ── Display name ─────────────────────────────────────────────────────────

    public static String getDisplayName(Context context) {
        return prefs(context).getString(KEY_NAME, "User");
    }

    public static void setDisplayName(Context context, String name) {
        prefs(context).edit().putString(KEY_NAME, name).apply();
    }

    // ── Currency symbol ───────────────────────────────────────────────────────

    public static String getCurrencySymbol(Context context) {
        return prefs(context).getString(KEY_CURRENCY, "₹");
    }

    public static void setCurrencySymbol(Context context, String symbol) {
        prefs(context).edit().putString(KEY_CURRENCY, symbol).apply();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
