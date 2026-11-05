package com.studybuddy.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Email and password submitted to log in. The 72-character limit matches what BCrypt reads. */
public record LoginRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 72) String password) {
}
