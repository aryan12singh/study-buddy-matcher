package com.studybuddy.common;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.common.error.AuthenticationRequiredException;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Internal account authorization policy; never an API response. */
@Component
public class AccountAccess {
    private final UserRepository users;
    private final StudentRepository students;

    public AccountAccess(UserRepository users, StudentRepository students) {
        this.users = users;
        this.students = students;
    }

    public Student requireStudent(Long actorId) {
        User actor = requireActive(actorId);
        if (actor.getRole() != Role.STUDENT) {
            throw new ForbiddenActionException("A student account is required");
        }
        return students.findById(actorId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Student " + actorId + " not found"));
    }

    public User requireAdmin(Long actorId) {
        User user = requireActive(actorId);
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenActionException("An administrator account is required");
        }
        return user;
    }

    public User requireActive(Long actorId) {
        if (actorId == null || actorId <= 0) {
            throw new AuthenticationRequiredException();
        }
        User actor = users.findById(actorId).orElseThrow(AuthenticationRequiredException::new);
        if (!actor.isActive()) {
            throw new AuthenticationRequiredException();
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
            && principal.id().equals(actorId) && principal.tokenVersion() != actor.getTokenVersion()) {
            throw new AuthenticationRequiredException();
        }
        return actor;
    }
}
