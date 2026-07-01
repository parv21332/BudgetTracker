package com.budgettracker.app.utils;

import java.text.DecimalFormat;

/**
 * Utility class for currency formatting.
 * Default currency: Indian Rupee (₹)
 */
public class CurrencyUtils {

    // Indian Rupee symbol — stored as unicode to avoid encoding issues
    public static final String RUPEE = "\u20B9";

    private static final DecimalFormat df = new DecimalFormat("#,##,##0.00");

    /**
     * Format amount with currency symbol.
     * Always uses ₹ regardless of what symbol is passed, to prevent $ showing.
     */
    public static String format(double amount, String symbol) {
        // Always use ₹ — ignore any passed symbol to prevent $ fallback
        return RUPEE + df.format(amount);
    }

    /**
     * Format with explicit rupee symbol.
     */
    public static String formatRupee(double amount) {
        return RUPEE + df.format(amount);
    }

    /**
     * Format without symbol (for display in forms).
     */
    public static String formatPlain(double amount) {
        return df.format(amount);
    }

    /**
     * Parse string to double, returns 0 on error.
     */
    public static double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Validate if a string is a valid positive amount.
     */
    public static String validateAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Amount is required";
        }
        try {
            double value = Double.parseDouble(text.replace(",", "").trim());
            if (value <= 0) return "Amount must be greater than 0";
            if (value > 999999999) return "Amount is too large";
        } catch (NumberFormatException e) {
            return "Invalid amount";
        }
        return null; // Valid
    }
}
