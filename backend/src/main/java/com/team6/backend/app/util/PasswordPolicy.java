package com.team6.backend.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Password strength rules for new accounts.
 * The frontend shows the same rules as a live checklist
 * (frontend_react/src/components/account/passwordRules.js) - keep the two in sync.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    /** BCrypt only uses the first 72 bytes, so anything longer would be silently ignored */
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {}

    /**
     * @return every rule the password breaks, as messages for the user; empty if it is fine
     */
    public static List<String> problems(String username, String password) {
        List<String> problems = new ArrayList<>();
        String pw = password == null ? "" : password;

        if (pw.length() < MIN_LENGTH) problems.add("be at least " + MIN_LENGTH + " characters long");
        if (pw.length() > MAX_LENGTH) problems.add("be no more than " + MAX_LENGTH + " characters long");
        if (pw.chars().noneMatch(Character::isUpperCase)) problems.add("include an uppercase letter");
        if (pw.chars().noneMatch(Character::isLowerCase)) problems.add("include a lowercase letter");
        if (pw.chars().noneMatch(Character::isDigit)) problems.add("include a number");
        if (pw.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            problems.add("include a special character (e.g. ! @ # $ %)");
        }
        if (pw.chars().anyMatch(Character::isWhitespace)) problems.add("not contain spaces");
        if (username != null && !username.isBlank()
                && pw.toLowerCase(Locale.ROOT).contains(username.trim().toLowerCase(Locale.ROOT))) {
            problems.add("not contain your username");
        }
        return problems;
    }

    /** one sentence for an error response, or null if the password is fine */
    public static String errorMessage(String username, String password) {
        List<String> problems = problems(username, password);
        if (problems.isEmpty()) return null;
        return "Password must " + String.join(", ", problems) + ".";
    }
}
