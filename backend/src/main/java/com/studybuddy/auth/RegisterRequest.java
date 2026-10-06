package com.studybuddy.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String school,
        @NotBlank @Size(max = 255) String programme,
        @NotNull @Min(1) Integer yearOfStudy,
        @NotBlank @Size(max = 255) String contactNumber) {
}
