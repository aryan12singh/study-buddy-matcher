package com.studybuddy.common;

import com.studybuddy.common.error.InvalidInputException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared limits match the persisted columns and the public forms. */
public final class InputRules {
    public static final int TEXT_LIMIT = 255;
    public static final int DESCRIPTION_LIMIT = 4000;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private InputRules() {
    }

    public static String email(String value) {
        String normal = required(value, "Email", TEXT_LIMIT).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normal).matches()) {
            throw new InvalidInputException("email", "A valid email is required");
        }
        return normal;
    }

    public static String password(String value) {
        if (value == null || value.length() < MIN_PASSWORD_LENGTH || value.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_LENGTH) {
            throw new InvalidInputException("password", "Password must contain at least 8 characters and at most 72 UTF-8 bytes");
        }
        return value;
    }

    public static String required(String value, String field, int limit) {
        String normal = optional(value, field, limit);
        if (normal == null) {
            throw new InvalidInputException(fieldKey(field), field + " is required");
        }
        return normal;
    }

    public static String optional(String value, String field, int limit) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normal = value.strip();
        if (normal.length() > limit) {
            throw new InvalidInputException(fieldKey(field), field + " must contain at most " + limit + " characters");
        }
        return normal;
    }

    private static String fieldKey(String label) {
        return switch (label) {
            case "Contact number" -> "contactNumber";
            case "Context course" -> "context.courseId";
            case "Receiver" -> "receiverId";
            case "Course" -> "courseId";
            case "Member" -> "studentId";
            default -> label.substring(0, 1).toLowerCase(Locale.ROOT) + label.substring(1);
        };
    }

    public static void year(Integer value) {
        if (value == null || value < 1) {
            throw new InvalidInputException("yearOfStudy", "Year of study must be at least 1");
        }
    }

    public static void positiveId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new InvalidInputException(fieldKey(field), field + " must be positive");
        }
    }
}
