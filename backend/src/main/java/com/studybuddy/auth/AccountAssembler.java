package com.studybuddy.auth;

import com.studybuddy.student.Student;
import com.studybuddy.user.User;
import org.springframework.stereotype.Component;

/**
 * Builds the {@link CurrentAccountDto} for a signed-in account, adding the student's name when the
 * account has a student profile.
 */
@Component
public class AccountAssembler {

    public CurrentAccountDto toDto(User user, Student student) {
        return new CurrentAccountDto(user.getId(), user.getEmail(), user.getRole(), student == null ? null : student.getName());
    }
}
