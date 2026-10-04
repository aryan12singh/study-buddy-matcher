package com.studybuddy.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * What an admin supplies when editing an account. The student profile fields
 * apply only to student accounts and are ignored for admin accounts. Active
 * status is not changed here; deactivating and reactivating are separate
 * actions. The annotations are for the controller's {@code @Valid}; the
 * service checks the rules again so it is safe to call without one.
 */
public record AdminUserUpdateRequest(
        @NotBlank @Email String email,
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        String contactNumber) {

    public static final int MIN_YEAR_OF_STUDY = 1;
}
