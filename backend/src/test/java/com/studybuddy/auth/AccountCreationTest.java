package com.studybuddy.auth;

import com.studybuddy.admin.DuplicateEmailException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCreationTest {
    @Mock private UserRepository users;
    @Mock private StudentRepository students;
    @Mock private PasswordEncoder passwords;
    private AccountCreation creation;
    @BeforeEach void setUp() { creation = new AccountCreation(users,students,passwords); }
    @Test void creationNormalizesEmailHashesPasswordAndBuildsSharedIdentityProfile() {
        when(passwords.encode("runtime-password")).thenReturn("encoded-test-value");
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            ReflectionTestUtils.setField(user,"id",12L);
            return user;
        });
        User user = creation.create(" Jamie@EXAMPLE.TEST ","runtime-password",Role.STUDENT," Jamie "," SCIS "," IS ",3," Synthetic contact ");
        var captured = ArgumentCaptor.forClass(Student.class);
        verify(students).saveAndFlush(captured.capture());
        assertEquals("jamie@example.test",user.getEmail());
        assertEquals("encoded-test-value",user.getPasswordHash());
        assertSame(user,captured.getValue().getUser());
        assertEquals("Jamie",captured.getValue().getName());
        assertEquals("Synthetic contact",captured.getValue().getContactNumber());
    }
    @Test void duplicatesAreRejectedBeforeHashingOrWriting() {
        when(users.existsByEmail("jamie@example.test")).thenReturn(true);
        assertThrows(DuplicateEmailException.class,() -> creation.create("JAMIE@example.test","runtime-password",Role.ADMIN,null,null,null,null,null));
        verify(users,never()).saveAndFlush(any());
        verifyNoInteractions(students,passwords);
    }
    @Test void studentFieldValidationRunsBeforeAnyWrite() {
        assertThrows(InvalidInputException.class,() -> creation.create("student@example.test","runtime-password",Role.STUDENT,null,"SCIS","IS",3,"contact"));
        assertThrows(InvalidInputException.class,() -> creation.create("student@example.test","runtime-password",Role.STUDENT,"Student","SCIS","IS",0,"contact"));
        verify(users,never()).saveAndFlush(any());
        verifyNoInteractions(students,passwords);
    }
    @Test void adminDoesNotCreateAStudentProfile() {
        when(passwords.encode("runtime-password")).thenReturn("encoded-test-value");
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));
        creation.create("admin@example.test","runtime-password",Role.ADMIN,null,null,null,null,null);
        verifyNoInteractions(students);
    }
}
