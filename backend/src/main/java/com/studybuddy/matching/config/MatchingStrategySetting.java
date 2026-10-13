package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Singleton settings row. Every mutation locks id 1 before changing any weights. */
@Entity
@Table(name = "matching_strategy_settings")
public class MatchingStrategySetting {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;
    @Enumerated(EnumType.STRING)
    @Column(name = "active_strategy", nullable = false, length = 20)
    private MatchingStrategyType activeStrategy;
    @Column(name = "min_score", nullable = false)
    private double minScore;
    @Column(name = "config_version", nullable = false)
    private long configVersion;
    @Column(name = "has_override", nullable = false)
    private boolean hasOverride;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "matching_enabled_criteria", joinColumns = @JoinColumn(name = "setting_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "criterion", nullable = false, length = 20)
    private Set<MatchingCriterion> enabledCriteria = EnumSet.noneOf(MatchingCriterion.class);
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected MatchingStrategySetting() { }

    public MatchingStrategySetting(MatchingStrategyType strategy) { activeStrategy = strategy; }
    public Long getId() { return id; }
    public MatchingStrategyType getActiveStrategy() { return activeStrategy; }
    public double getMinScore() { return minScore; }
    public long getConfigVersion() { return configVersion; }
    public boolean hasOverride() { return hasOverride; }
    public Set<MatchingCriterion> getEnabledCriteria() { return Set.copyOf(enabledCriteria); }

    public void saveOverride(MatchingStrategyType strategy, Set<MatchingCriterion> enabled, double threshold) {
        activeStrategy = strategy;
        enabledCriteria.clear();
        enabledCriteria.addAll(enabled);
        minScore = threshold;
        hasOverride = true;
        changed();
    }

    public void restoreDefaults() {
        hasOverride = false;
        enabledCriteria.clear();
        changed();
    }

    private void changed() {
        configVersion++;
        updatedAt = LocalDateTime.now();
    }
}
