package com.studybuddy.auth;

import com.studybuddy.user.Role;

public record CurrentAccountDto(
        Long id,
        String email,
        Role role,
        String name) {
}
