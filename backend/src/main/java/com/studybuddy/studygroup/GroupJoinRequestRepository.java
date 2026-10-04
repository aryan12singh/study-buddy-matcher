package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByStudyGroupIdAndStatus(Long studyGroupId, GroupJoinRequestStatus status);

    List<GroupJoinRequest> findByStudentId(Long studentId);

    List<GroupJoinRequest> findByStudentIdAndStatus(Long studentId, GroupJoinRequestStatus status);

    List<GroupJoinRequest> findByStudyGroupIdAndStatusOrderByCreatedAtDesc(Long studyGroupId,
                                                                           GroupJoinRequestStatus status);

    boolean existsByStudyGroupIdAndStudentIdAndStatus(Long studyGroupId, Long studentId,
                                                      GroupJoinRequestStatus status);
}
