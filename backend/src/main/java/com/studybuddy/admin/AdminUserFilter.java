package com.studybuddy.admin;

import com.studybuddy.student.Student;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;

import java.util.Locale;

/**
 * Optional filters for the admin user list. A null field means "any".
 * {@code search} matches part of the email or the student's name, ignoring
 * case. Filtering happens in Java, as with the study group browse filter, so
 * each rule is readable and unit-testable.
 */
public record AdminUserFilter(Role role, Boolean active, String search) {

    public static AdminUserFilter none() {
        return new AdminUserFilter(null, null, null);
    }

    /**
     * @param student the user's student profile, or null for an admin account
     */
    public boolean matches(User user, Student student) {
        return matchesRole(user) && matchesActive(user) && matchesSearch(user, student);
    }

    private boolean matchesRole(User user) {
        return role == null || role == user.getRole();
    }

    private boolean matchesActive(User user) {
        return active == null || active == user.isActive();
    }

    private boolean matchesSearch(User user, Student student) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.strip().toLowerCase(Locale.ROOT);
        return contains(user.getEmail(), term) || (student != null && contains(student.getName(), term));
    }

    private static boolean contains(String text, String term) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(term);
    }
}
