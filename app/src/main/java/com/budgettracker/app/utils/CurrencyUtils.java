package com.budgettracker.app.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for currency formatting.
 */
public class CurrencyUtils {

    private static final DecimalFormat df = new DecimalFormat("#,##,##0.00");

    /**
     * Format amount with currency symbol.
     * @param amount value
     * @param symbol currency symbol (e.g., "₹", "$")
     */
    public static String format(double amount, String symbol) {
        return symbol + df.format(amount);
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
