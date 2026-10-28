package com.studybuddy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStudyGroupIdOrderByCreatedAtDescIdDesc(Long groupId);
    List<Quiz> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);
}
