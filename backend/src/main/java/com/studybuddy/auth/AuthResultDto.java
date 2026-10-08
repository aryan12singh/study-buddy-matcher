package com.studybuddy.auth;

import java.time.Instant;

public record AuthResultDto(String token, Instant expiresAt, CurrentAccountDto account) {
}
