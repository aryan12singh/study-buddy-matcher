package com.studybuddy.common;

import com.studybuddy.admin.UserNotFoundException;
import com.studybuddy.common.error.AuthenticationRequiredException;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Internal account authorization and row-lock policy; never an API response. */
@Component
public class AccountAccess {
    private final UserRepository users;
    private final StudentRepository students;
    private final DatabaseMutationLock mutationLock;

    public AccountAccess(UserRepository users, StudentRepository students, DatabaseMutationLock mutationLock) {
        this.users = users;
        this.students = students;
        this.mutationLock = mutationLock;
    }

    public void beginWrite() {
        mutationLock.shared();
    }

    public Student requireStudent(Long actorId) {
        User actor = requireActive(actorId);
        if (actor.getRole() != Role.STUDENT) {
            throw new ForbiddenActionException("A student account is required");
        }
        return students.findById(actorId).orElseThrow(() -> new StudentNotFoundException(actorId));
    }

    public Student eligibleStudent(Long id) {
        Student student = students.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
        if (!student.getUser().isActive() || student.getUser().getRole() != Role.STUDENT) {
            throw new StudentNotFoundException(id);
        }
        return student;
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

    /** Lock account rows in increasing order before any group/request row, and require every affected student to be active. */
    public void lockStudents(Long actorId, Long... affectedIds) {
        lockAccounts(actorId, affectedIds);
        for (Long id : affectedIds) {
            if (!id.equals(actorId)) {
                eligibleStudent(id);
            }
        }
    }

    /** Lock account rows in increasing order and require an active student actor; other accounts are not checked. */
    public void lockAccounts(Long actorId, Long... affectedIds) {
        mutationLock.shared();
        if (actorId == null || actorId <= 0) {
            throw new AuthenticationRequiredException();
        }
        Arrays.stream(affectedIds).distinct().sorted().forEach(id ->
                users.findByIdForUpdate(id).orElseThrow(() -> id.equals(actorId)
                        ? new AuthenticationRequiredException() : new StudentNotFoundException(id)));
        requireStudent(actorId);
    }

    public User lockAdmin(Long actorId) {
        mutationLock.shared();
        users.findByIdForUpdate(actorId).orElseThrow(AuthenticationRequiredException::new);
        return requireAdmin(actorId);
    }

    public User lockAdminTarget(Long actorId, Long targetId) {
        mutationLock.shared();
        Stream.of(actorId, targetId).distinct().sorted().forEach(id ->
                users.findByIdForUpdate(id).orElseThrow(() -> id.equals(actorId)
                        ? new AuthenticationRequiredException() : new UserNotFoundException(id)));
        requireAdmin(actorId);
        return users.findById(targetId).orElseThrow(() -> new UserNotFoundException(targetId));
    }

    /** Serializes all status/removal operations and then locks/recounts admins. */
    public User lockLifecycle(Long actorId, Long targetId) {
        mutationLock.exclusive();
        users.findAllAdministratorsForUpdate();
        requireAdmin(actorId);
        return users.findByIdForUpdate(targetId).orElseThrow(() -> new UserNotFoundException(targetId));
    }

    public List<Long> eligibleStudentIds() {
        return students.findActiveIds();
    }
}
