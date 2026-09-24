package com.studybuddy.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * The single currently-active matching strategy. Expected to hold exactly
 * one row; the admin UI updates it rather than inserting new ones.
 */
@Entity
@Table(name = "matching_strategy_settings")
public class MatchingStrategySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_strategy", nullable = false, length = 20)
    private MatchingStrategyType activeStrategy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected MatchingStrategySetting() {
    }

    public MatchingStrategySetting(MatchingStrategyType activeStrategy) {
        this.activeStrategy = activeStrategy;
    }

    public Long getId() {
        return id;
    }

    public MatchingStrategyType getActiveStrategy() {
        return activeStrategy;
    }

    public void setActiveStrategy(MatchingStrategyType activeStrategy) {
        this.activeStrategy = activeStrategy;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
