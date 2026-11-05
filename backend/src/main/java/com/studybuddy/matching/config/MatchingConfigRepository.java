package com.studybuddy.matching.config;

import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Stores the administrator's saved weight for each strategy and criterion pair. */
public interface MatchingConfigRepository extends JpaRepository<MatchingConfig, Long> {

    Optional<MatchingConfig> findByStrategyAndCriterion(MatchingStrategyType strategy, MatchingCriterion criterion);
}
