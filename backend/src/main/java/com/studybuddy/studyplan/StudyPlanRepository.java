package com.studybuddy.studyplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Finds a student's personal study plans, newest first, and the roadmaps generated for a study
 * session.
 */
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);
    List<StudyPlan> findBySessionIdOrderByFirstBlockDateAscIdAsc(Long sessionId);
    Optional<StudyPlan> findFirstBySessionIdOrderByCreatedAtDescIdDesc(Long sessionId);
}
