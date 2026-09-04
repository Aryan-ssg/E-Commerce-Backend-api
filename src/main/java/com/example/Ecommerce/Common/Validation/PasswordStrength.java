package com.example.Ecommerce.Common.Validation;

/**
 * Mirrors frontend/src/utils/passwordStrength.ts so backend and frontend
 * agree on what "Fair" (score >= 2) means.
 *
 * Scoring (0-4):
 *  0 = Too weak (<4 chars or empty)
 *  1 = Weak (length <8 OR length>=8 but only 1 char type)
 *  2 = Fair (length>=8 + exactly 2 types)  ← minimum for registration
 *  3 = Good (length>=8 + 3 types, or 4 types without 12+, or 2 types+12+)
 *  4 = Strong (length>=12 + 4 types)
 */
public final class PasswordStrength {

    private PasswordStrength() {}

    public static int score(String password) {
        if (password == null || password.isEmpty()) return 0;

        boolean length = password.length() >= 8;
        boolean lower = password.matches(".*[a-z].*");
        boolean upper = password.matches(".*[A-Z].*");
        boolean digit = password.matches(".*[0-9].*");
        boolean special = password.matches(".*[^A-Za-z0-9].*");
        boolean longEnough = password.length() >= 12;

        int typeCount = 0;
        if (lower) typeCount++;
        if (upper) typeCount++;
        if (digit) typeCount++;
        if (special) typeCount++;

        int score;
        if (password.length() < 4) {
            score = 0;
        } else if (!length) {
            score = 1;
        } else if (typeCount <= 1) {
            score = 1;
        } else if (typeCount == 2) {
            score = 2;
        } else if (typeCount == 3) {
            score = 3;
        } else { // typeCount >=4
            score = longEnough ? 4 : 3;
        }

        if (longEnough && score == 2) {
            score = 3; // 12+ bumps Fair -> Good
        }
        return score;
    }

    public static boolean isAtLeastFair(String password) {
        return score(password) >= 2;
    }

    public static String label(int score) {
        return switch (score) {
            case 0 -> "Too weak";
            case 1 -> "Weak";
            case 2 -> "Fair";
            case 3 -> "Good";
            case 4 -> "Strong";
            default -> "Too weak";
        };
    }
}
