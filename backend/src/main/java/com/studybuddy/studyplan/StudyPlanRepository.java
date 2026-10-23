package com.studybuddy.studyplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);
    List<StudyPlan> findBySessionIdOrderByFirstBlockDateAscIdAsc(Long sessionId);
}
