package com.studybuddy.quiz;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Finds a group's quizzes and a student's personal quizzes, newest first. */
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    /** A list shows each quiz's question count and course, so both load with the quizzes in one query. */
    @EntityGraph(attributePaths = {"questions", "course", "studyGroup", "session"})
    List<Quiz> findByStudyGroupIdOrderByCreatedAtDescIdDesc(Long groupId);
    @EntityGraph(attributePaths = {"questions", "course", "studyGroup", "session"})
    List<Quiz> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);
}
