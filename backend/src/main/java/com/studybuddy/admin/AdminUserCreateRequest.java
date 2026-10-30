package com.studybuddy.admin;

import com.studybuddy.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Account details an administrator submits to create a student or administrator account. The
 * profile fields are optional here because an administrator account has no student profile; the
 * service requires them when the role is {@code STUDENT}.
 */
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
