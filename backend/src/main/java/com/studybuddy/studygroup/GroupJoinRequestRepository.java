package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByStudyGroupIdAndStatus(Long studyGroupId, GroupJoinRequestStatus status);

    List<GroupJoinRequest> findByStudentId(Long studentId);
}
