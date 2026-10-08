package com.studybuddy.admin;

import com.studybuddy.student.Student;
import com.studybuddy.user.User;
import org.springframework.stereotype.Component;

/**
 * Converts a {@link User}, with its student profile if it has one, into the
 * admin DTOs so neither entity leaves the service layer.
 */
@Component
public class AdminUserAssembler {
    /**
     * @param studentName the student's name, or null for an admin account
     */
    public AdminUserSummaryDto toSummary(User user, String studentName) {
        return new AdminUserSummaryDto(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            studentName,
            user.isActive(),
            user.getCreatedAt(), user.getLastLoginAt());
    }

    /**
     * @param student the user's student profile, or null for an admin account
     * @param usage the student's usage, or null for an admin account
     */
    public AdminUserDetailDto toDetail(User user, Student student, UserUsageDto usage) {
        return new AdminUserDetailDto(toSummary(user, student == null ? null : student.getName()), usage,
            student == null ? null : new AdminStudentProfileDto(student.getName(), student.getSchool(), student.getProgramme(), student.getYearOfStudy()));
    }
}
