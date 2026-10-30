package com.studybuddy.auth;

import com.studybuddy.user.Role;

/**
 * Who is signed in: account id, email, role and, for a student, their name. Never carries the
 * contact number or password hash.
 */
public record CurrentAccountDto(
        Long id,
        String email,
        Role role,
        String name) {
}
