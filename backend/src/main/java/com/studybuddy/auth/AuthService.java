package com.studybuddy.auth;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.ApiException;
import com.studybuddy.security.JwtTokenService;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwords;
    private final JwtTokenService tokens;
    private final AccountCreation creation;
    private final AccountAssembler assembler;
    private final AccountAccess access;
    private final DatabaseMutationLock mutationLock;

    public AuthService(UserRepository users, StudentRepository students, PasswordEncoder passwords,
        JwtTokenService tokens, AccountCreation creation, AccountAssembler assembler,
        AccountAccess access, DatabaseMutationLock mutationLock) {
        this.users = users;
        this.students = students;
        this.passwords = passwords;
        this.tokens = tokens;
        this.creation = creation;
        this.assembler = assembler;
        this.access = access;
        this.mutationLock = mutationLock;
    }

    public AuthResultDto login(LoginRequest request) {
        mutationLock.shared();
        String email = InputRules.email(request.email());
        // Lock the identity lookup itself so a waiting login cannot retain an older managed User.
        User user = users.findByEmailForUpdate(email).orElseThrow(AuthService::invalidCredentials);
        if (!user.isActive() || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        user.recordLogin();
        return signedIn(user);
    }

    public AuthResultDto register(RegisterRequest request) {
        mutationLock.shared();
        User user = creation.create(request.email(), request.password(), Role.STUDENT, request.name(), request.school(),
            request.programme(), request.yearOfStudy(), request.contactNumber());
        user.recordLogin();
        return signedIn(user);
    }

    @Transactional(readOnly = true)
    public CurrentAccountDto me(Long actorId) {
        User user = access.requireActive(actorId);
        return assembler.toDto(user, students.findById(actorId).orElse(null));
    }

    private AuthResultDto signedIn(User user) {
        var expiry = tokens.expiresAt();
        return new AuthResultDto(tokens.issue(user, expiry), expiry,
            assembler.toDto(user, students.findById(user.getId()).orElse(null)));
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    }
}
