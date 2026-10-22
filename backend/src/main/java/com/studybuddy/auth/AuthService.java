package com.studybuddy.auth;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.ApiException;
import com.studybuddy.common.error.AuthenticationRequiredException;
import com.studybuddy.security.JwtTokenService;
import com.studybuddy.security.LoginThrottle;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
    private final LoginThrottle loginThrottle;
    /** Compared against when the email is unknown, so every login within the length limit costs one BCrypt check. */
    private final String unknownAccountHash;

    public AuthService(UserRepository users, StudentRepository students, PasswordEncoder passwords,
        JwtTokenService tokens, AccountCreation creation, AccountAssembler assembler,
        AccountAccess access, DatabaseMutationLock mutationLock, LoginThrottle loginThrottle) {
        this.users = users;
        this.students = students;
        this.passwords = passwords;
        this.tokens = tokens;
        this.creation = creation;
        this.assembler = assembler;
        this.access = access;
        this.mutationLock = mutationLock;
        this.loginThrottle = loginThrottle;
        this.unknownAccountHash = passwords.encode(UUID.randomUUID().toString());
    }

    public AuthResultDto login(LoginRequest request) {
        String email = InputRules.email(request.email());
        loginThrottle.beginAttempt(email);
        mutationLock.shared();
        // Lock the identity lookup itself so a waiting login cannot retain an older managed User.
        User user = users.findByEmailForUpdate(email).orElse(null);
        // An unknown email still runs BCrypt, so response time does not reveal which emails are registered.
        boolean passwordMatches = request.password().getBytes(StandardCharsets.UTF_8).length <= InputRules.MAX_PASSWORD_LENGTH
                && passwords.matches(request.password(), user == null ? unknownAccountHash : user.getPasswordHash());
        if (user == null || !passwordMatches) {
            throw invalidCredentials();
        }
        loginThrottle.recordSuccess(email);
        // Checked after the password, so only the account holder learns why they cannot sign in
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED",
                "This account has been deactivated. Please contact an administrator.");
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

    /** Ends every session of this account: tokens issued before now stop being accepted. */
    public void logout(Long actorId) {
        mutationLock.shared();
        users.findByIdForUpdate(actorId).orElseThrow(AuthenticationRequiredException::new);
        access.requireActive(actorId).revokeTokens();
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
