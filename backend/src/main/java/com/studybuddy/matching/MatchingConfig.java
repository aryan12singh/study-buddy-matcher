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
 * The admin-tunable weight for one matching criterion. A starting shape for
 * Team A's scorers to read from; refine as the matching engine needs it.
 */
@Entity
@Table(name = "matching_configs")
public class MatchingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private MatchingCriterion criterion;

    @Column(nullable = false)
    private double weight;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected MatchingConfig() {
    }

    public MatchingConfig(MatchingCriterion criterion, double weight) {
        this.criterion = criterion;
        this.weight = weight;
    }

    public Long getId() {
        return id;
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
