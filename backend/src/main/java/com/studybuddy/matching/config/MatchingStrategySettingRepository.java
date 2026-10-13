package com.studybuddy.matching.config;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MatchingStrategySettingRepository extends JpaRepository<MatchingStrategySetting, Long> {
    Optional<MatchingStrategySetting> findFirstByOrderByIdAsc();

    /** Also works after a disposable test truncation; concurrent first saves serialize on the same row. */
    @Modifying
    @Query(value = "insert into matching_strategy_settings(id,active_strategy,updated_at,min_score,config_version,has_override) "
            + "values (1,:strategy,now(),0,0,false) on conflict(id) do nothing", nativeQuery = true)
    void initialize(@Param("strategy") String strategy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select setting from MatchingStrategySetting setting where setting.id=1")
    Optional<MatchingStrategySetting> lockSingleton();
}
