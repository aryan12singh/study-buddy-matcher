package com.studybuddy.security;

import com.studybuddy.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {
    private final SecretKey key;
    private final AuthProperties properties;

    public JwtTokenService(AuthProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public Instant expiresAt() {
        return Instant.now().plus(properties.tokenLifetime());
    }

    public String issue(User user, Instant expiry) {
        return Jwts.builder().subject(user.getId().toString())
            .claim("role", user.getRole().name()).claim("version", user.getTokenVersion())
            .issuedAt(Date.from(Instant.now())).expiration(Date.from(expiry)).signWith(key).compact();
    }

    public VerifiedToken verify(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new VerifiedToken(Long.valueOf(claims.getSubject()),
            claims.get("version", Number.class).longValue(), claims.get("role", String.class));
    }

    public record VerifiedToken(Long userId, long tokenVersion, String role) {
    }
}
