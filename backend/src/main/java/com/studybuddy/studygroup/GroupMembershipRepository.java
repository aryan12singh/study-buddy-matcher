package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    List<GroupMembership> findByStudyGroupId(Long studyGroupId);

    List<GroupMembership> findByStudentId(Long studentId);

    List<GroupMembership> findByStudyGroupIdOrderByJoinedAtAsc(Long studyGroupId);

    Optional<GroupMembership> findByStudyGroupIdAndStudentId(Long studyGroupId, Long studentId);

    boolean existsByStudyGroupIdAndStudentId(Long studyGroupId, Long studentId);

    /** Members of the group, leader included. */
    long countByStudyGroupId(Long studyGroupId);
}
