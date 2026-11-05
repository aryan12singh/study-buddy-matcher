package com.studybuddy.auth;

import java.time.Instant;

/**
 * Returned after a successful login or registration: the signed JWT, when it expires, and the
 * account it belongs to.
 */
public record AuthResultDto(String token, Instant expiresAt, CurrentAccountDto account) {
}
