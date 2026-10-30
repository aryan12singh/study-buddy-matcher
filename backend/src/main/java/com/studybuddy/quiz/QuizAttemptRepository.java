package com.studybuddy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Stores each student's attempt at a quiz and finds the submitted ones for the leaderboard. */
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByQuizIdAndStudentId(Long quizId, Long studentId);
    List<QuizAttempt> findByQuizIdAndSubmittedAtIsNotNull(Long quizId);
}
