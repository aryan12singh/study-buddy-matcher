package com.studybuddy.studyplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
    Optional<StudyPlan> findByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<StudyPlan> findBySessionId(Long sessionId);
}
