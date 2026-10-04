package com.studybuddy.admin;

import com.studybuddy.user.Role;

import java.time.LocalDateTime;

/**
 * One account in the admin user list. {@code name} is null for admin
 * accounts, which have no student profile. No password hash and no contact
 * number.
 */
public record AdminUserSummaryDto(
        Long id,
        String email,
        Role role,
        String name,
        boolean active,
        LocalDateTime createdAt) {
}
