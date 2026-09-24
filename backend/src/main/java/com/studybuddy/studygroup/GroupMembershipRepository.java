package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    List<GroupMembership> findByStudyGroupId(Long studyGroupId);

    List<GroupMembership> findByStudentId(Long studentId);
}
