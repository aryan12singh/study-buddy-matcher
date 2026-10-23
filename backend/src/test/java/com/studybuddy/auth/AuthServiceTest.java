package com.studybuddy.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.error.ApiException;
import com.studybuddy.security.JwtTokenService;
import com.studybuddy.security.LoginThrottle;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository users;
    @Mock private StudentRepository students;
    @Mock private PasswordEncoder passwords;
    @Mock private JwtTokenService tokens;
    @Mock private AccountCreation creation;
    @Mock private AccountAssembler assembler;
    @Mock private AccountAccess access;
    @Mock private DatabaseMutationLock mutationLock;
    @Mock private LoginThrottle loginThrottle;
    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwords.encode(anyString())).thenReturn("unknown-account-hash");
        service = new AuthService(users, students, passwords, tokens, creation, assembler, access, mutationLock, loginThrottle);
    }

    @Test
    void unknownEmailStillPaysForOnePasswordComparison() {
        when(users.findByEmailForUpdate("nobody@example.test")).thenReturn(Optional.empty());

        var rejected = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("nobody@example.test", "some-password")));

        assertEquals("INVALID_CREDENTIALS", rejected.getCode());
        verify(passwords).matches(eq("some-password"), eq("unknown-account-hash"));
        verify(loginThrottle, never()).recordSuccess(anyString());
    }
}
