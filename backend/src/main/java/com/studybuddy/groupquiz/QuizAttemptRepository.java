package com.studybuddy.groupquiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByQuizIdAndStudentId(Long quizId, Long studentId);
    List<QuizAttempt> findByQuizIdAndSubmittedAtIsNotNull(Long quizId);
    long countByQuizIdAndSubmittedAtIsNotNull(Long quizId);
}
