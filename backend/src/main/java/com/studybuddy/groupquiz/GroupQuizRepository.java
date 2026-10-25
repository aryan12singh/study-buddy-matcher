package com.studybuddy.groupquiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupQuizRepository extends JpaRepository<GroupQuiz, Long> {
    List<GroupQuiz> findByStudyGroupIdOrderByCreatedAtDescIdDesc(Long groupId);
}
