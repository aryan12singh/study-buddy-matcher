package com.studybuddy.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Authentication identity. Profile data lives on {@code Student}, not here,
 * so an admin account never carries fields it has no use for.
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public long getTokenVersion() {
        return tokenVersion;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void recordLogin() {
        lastLoginAt = Instant.now();
    }

    public void deactivate() {
        active = false;
        tokenVersion++;
    }

    protected User() {
    }

    public User(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (!active && this.active) {
            deactivate();
            return;
        }
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
