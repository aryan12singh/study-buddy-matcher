package com.studybuddy.security;

import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    private JwtTokenService tokens;
    private User user;
    @BeforeEach void setUp() {
        tokens = new JwtTokenService(new AuthProperties(UUID.randomUUID().toString()+UUID.randomUUID(),Duration.ofHours(1),List.of("http://localhost:5173")));
        user = new User("student@example.test","test-hash",Role.STUDENT);
        ReflectionTestUtils.setField(user,"id",12L);
    }
    @Test void tokenCarriesAccountIdentityRoleAndRevocationVersion() {
        user.deactivate();user.setActive(true);
        var verified = tokens.verify(tokens.issue(user,Instant.now().plusSeconds(60)));
        assertEquals(12L,verified.userId());
        assertEquals("STUDENT",verified.role());
        assertEquals(1,verified.tokenVersion());
    }
    @Test void expiredTokenIsRejected() {
        assertThrows(ExpiredJwtException.class,() -> tokens.verify(tokens.issue(user,Instant.now().minusSeconds(60))));
    }
    @Test void tokenSignedWithDifferentRuntimeKeyIsRejected() {
        var other = new JwtTokenService(new AuthProperties(UUID.randomUUID().toString()+UUID.randomUUID(),Duration.ofHours(1),List.of("http://localhost:5173")));
        String token = other.issue(user,Instant.now().plusSeconds(60));
        assertThrows(JwtException.class,() -> tokens.verify(token));
    }
}
