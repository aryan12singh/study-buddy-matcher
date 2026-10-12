package com.studybuddy.matching.config;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchingStrategySettingRepository extends JpaRepository<MatchingStrategySetting, Long> {

    Optional<MatchingStrategySetting> findFirstByOrderByIdAsc();
}
