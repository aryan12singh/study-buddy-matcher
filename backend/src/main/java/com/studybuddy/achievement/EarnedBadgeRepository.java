package com.studybuddy.achievement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EarnedBadgeRepository extends JpaRepository<EarnedBadge, Long> {
    List<EarnedBadge> findByStudentId(Long studentId);

    /**
     * Saves a tier for a badge, or raises the saved one. The WHERE clause makes the database keep
     * the higher tier, so two requests at once can never lower it.
     */
    @Modifying
    @Query(value = "insert into earned_badges(student_id,badge,best_tier) values (:studentId,:badge,:tier) "
            + "on conflict(student_id,badge) do update set best_tier=excluded.best_tier, best_tier_reached_at=now() "
            + "where array_position(array['BRONZE','SILVER','GOLD']::varchar[], excluded.best_tier) "
            + "> array_position(array['BRONZE','SILVER','GOLD']::varchar[], earned_badges.best_tier)", nativeQuery = true)
    void raise(@Param("studentId") Long studentId, @Param("badge") String badge, @Param("tier") String tier);
}
