package com.budgettracker.app.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing using SHA-256 with salt.
 * Completely offline - no external dependencies.
 */
public class PasswordUtils {

    private static final String HASH_ALGORITHM = "SHA-256";
    // Fixed app-level salt (in production use per-user random salt stored in DB)
    private static final String APP_SALT = "BudgetTracker@2024#SecureLocal";

    /**
     * Hash a password using SHA-256 with salt.
     * @param password plain text password
     * @return hex-encoded hash string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String saltedPassword = APP_SALT + password + APP_SALT;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on Android
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verify a password against its hash.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        String computedHash = hashPassword(password);
        return computedHash.equals(storedHash);
    }

    /**
     * Validate password strength.
     * @return error message or null if valid
     */
    public static String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (password.length() > 50) {
            return "Password must be less than 50 characters";
        }
        return null; // Valid
    }

    /**
     * Convert byte array to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
