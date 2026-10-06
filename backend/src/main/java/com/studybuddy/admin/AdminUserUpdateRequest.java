package com.studybuddy.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What an admin supplies when editing an account. The student profile fields
 * apply only to student accounts and are ignored for admin accounts. Active
 * status is not changed here; deactivating and reactivating are separate
 * actions. The annotations are for the controller's {@code @Valid}; the
 * service checks the rules again so it is safe to call without one.
 */
public record AdminUserUpdateRequest(
        @NotBlank @Size(max = 255) String email,
        @Size(max = 255) String name,
        @Size(max = 255) String school,
        @Size(max = 255) String programme,
        Integer yearOfStudy,
        @Size(max = 255) String contactNumber) {
    public static final int MIN_YEAR_OF_STUDY = 1;
}
