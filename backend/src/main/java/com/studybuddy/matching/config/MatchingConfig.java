package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * The admin-tunable weight one matching strategy gives one criterion. Each
 * strategy keeps its own set, so there is one row per strategy and criterion.
 */
@Entity
@Table(name = "matching_configs", uniqueConstraints = @UniqueConstraint(columnNames = {"strategy", "criterion"}))
public class MatchingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchingStrategyType strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchingCriterion criterion;

    @Column(nullable = false)
    private double weight;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected MatchingConfig() {
    }

    public MatchingConfig(MatchingStrategyType strategy, MatchingCriterion criterion, double weight) {
        this.strategy = strategy;
        this.criterion = criterion;
        this.weight = weight;
    }

    public Long getId() {
        return id;
    }

    public MatchingStrategyType getStrategy() {
        return strategy;
    }

    public MatchingCriterion getCriterion() {
        return criterion;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
