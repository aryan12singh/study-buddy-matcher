package com.studybuddy.admin;

import com.studybuddy.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @Size(max = 255) String name,
        @Size(max = 255) String school,
        @Size(max = 255) String programme,
        Integer yearOfStudy,
        @Size(max = 255) String contactNumber) {
}
