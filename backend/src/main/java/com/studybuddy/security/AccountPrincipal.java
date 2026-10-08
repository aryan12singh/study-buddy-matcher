package com.studybuddy.security;

import com.studybuddy.user.Role;

/** Only verified account identity goes into the security context. */
public record AccountPrincipal(Long id, Role role, long tokenVersion) {
}
