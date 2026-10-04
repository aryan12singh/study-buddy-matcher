package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    /** Open groups, newest first. Browse filters are applied by {@link StudyGroupFilter}. */
    List<StudyGroup> findByActiveTrueOrderByCreatedAtDesc();

    List<StudyGroup> findByLeaderIdAndActiveTrue(Long leaderId);

    /** Every group the student has led, open or closed. */
    long countByLeaderId(Long leaderId);
}
