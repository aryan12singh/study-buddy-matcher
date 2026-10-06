package com.studybuddy.common;

import com.studybuddy.common.error.AuthenticationRequiredException;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountAccessTest {
    @Mock private UserRepository users;
    @Mock private StudentRepository students;
    @Mock private DatabaseMutationLock mutationLock;
    private AccountAccess access;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        access = new AccountAccess(users,students,mutationLock);
        alice = account(1L,Role.STUDENT);
        bob = account(2L,Role.STUDENT);
    }
    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void inactiveActorCannotUseAnyStudentService() {
        alice.deactivate();
        when(users.findById(1L)).thenReturn(Optional.of(alice));
        assertThrows(AuthenticationRequiredException.class,() -> access.requireStudent(1L));
        verifyNoInteractions(students);
    }
    @Test
    void missingOrInvalidActorCannotUseService() {
        assertThrows(AuthenticationRequiredException.class,() -> access.requireStudent(99L));
        assertThrows(AuthenticationRequiredException.class,() -> access.requireStudent(null));
    }
    @Test
    void adminCannotActAsStudent() {
        when(users.findById(1L)).thenReturn(Optional.of(account(1L,Role.ADMIN)));
        assertThrows(ForbiddenActionException.class,() -> access.requireStudent(1L));
    }
    @Test
    void studentCannotPerformAdminOperation() {
        when(users.findById(1L)).thenReturn(Optional.of(alice));
        assertThrows(ForbiddenActionException.class,() -> access.requireAdmin(1L));
    }
    @Test
    void inactiveSubjectIsUnavailable() {
        bob.deactivate();
        when(students.findById(2L)).thenReturn(Optional.of(profile(bob)));
        assertThrows(StudentNotFoundException.class,() -> access.eligibleStudent(2L));
    }
    @Test
    void reactivatedAccountRejectsPrincipalFromBeforeDeactivation() {
        alice.deactivate();
        alice.setActive(true);
        when(users.findById(1L)).thenReturn(Optional.of(alice));
        var principal = new AccountPrincipal(1L,Role.STUDENT,0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,java.util.List.of()));
        assertThrows(AuthenticationRequiredException.class,() -> access.requireActive(1L));
    }
    @Test
    void pairWritesLockDatabaseThenUsersInIncreasingOrder() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(alice));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(bob));
        when(users.findById(2L)).thenReturn(Optional.of(bob));
        when(students.findById(1L)).thenReturn(Optional.of(profile(alice)));
        when(students.findById(2L)).thenReturn(Optional.of(profile(bob)));
        access.lockStudents(2L,2L,1L,2L);
        var order = inOrder(mutationLock,users);
        order.verify(mutationLock).shared();
        order.verify(users).findByIdForUpdate(1L);
        order.verify(users).findByIdForUpdate(2L);
        order.verify(users).findById(2L);
    }
    @Test
    void lifecycleAcquiresExclusiveLockBeforeAdminAuthorizationAndTargetLookup() {
        var admin = account(3L,Role.ADMIN);
        when(users.findById(3L)).thenReturn(Optional.of(admin));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(alice));
        assertSame(alice,access.lockLifecycle(3L,1L));
        var order = inOrder(mutationLock,users);
        order.verify(mutationLock).exclusive();
        order.verify(users).findAllAdministratorsForUpdate();
        order.verify(users).findById(3L);
        order.verify(users).findByIdForUpdate(1L);
    }
    private static User account(Long id,Role role) {
        var user = new User("user"+id+"@example.test","test-hash",role);
        ReflectionTestUtils.setField(user,"id",id);
        return user;
    }
    private static Student profile(User user) {
        var student = new Student(user,"Student","SCIS","IS",3,"Synthetic contact");
        ReflectionTestUtils.setField(student,"id",user.getId());
        return student;
    }
}
