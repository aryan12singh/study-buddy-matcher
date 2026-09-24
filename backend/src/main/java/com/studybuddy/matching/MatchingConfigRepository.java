package com.studybuddy.matching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchingConfigRepository extends JpaRepository<MatchingConfig, Long> {

    Optional<MatchingConfig> findByCriterion(MatchingCriterion criterion);
}
