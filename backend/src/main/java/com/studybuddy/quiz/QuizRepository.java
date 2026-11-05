package com.studybuddy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Finds a group's quizzes and a student's personal quizzes, newest first. */
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStudyGroupIdOrderByCreatedAtDescIdDesc(Long groupId);
    List<Quiz> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);
}
