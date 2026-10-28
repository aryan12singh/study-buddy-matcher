package com.studybuddy.achievement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * The highest tier a student has reached for one badge. Rows are written only by
 * {@link EarnedBadgeRepository#raise}, which never lowers a tier, so this entity has no setters.
 */
@Entity
@Table(name = "earned_badges", uniqueConstraints =
        @UniqueConstraint(name = "earned_badges_student_badge_key", columnNames = {"student_id", "badge"}))
public class EarnedBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BadgeDefinition badge;

    @Enumerated(EnumType.STRING)
    @Column(name = "best_tier", nullable = false, length = 8)
    private BadgeTier bestTier;

    @Column(name = "first_earned_at", nullable = false)
    private Instant firstEarnedAt;

    @Column(name = "best_tier_reached_at", nullable = false)
    private Instant bestTierReachedAt;

    protected EarnedBadge() {
    }

    public BadgeDefinition getBadge() {
        return badge;
    }

    public BadgeTier getBestTier() {
        return bestTier;
    }
}
