package com.studybuddy.studygroup;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GroupJoinRequest r where r.id=:id")
    Optional<GroupJoinRequest> findByIdForUpdate(@Param("id") Long id);

    @Query("select r.student.id from GroupJoinRequest r where r.id=:id and r.studyGroup.id=:groupId")
    Optional<Long> findApplicantId(@Param("id") Long id, @Param("groupId") Long groupId);
    Optional<GroupJoinRequest> findFirstByStudyGroupIdAndStudentIdOrderByCreatedAtDescIdDesc(Long groupId, Long studentId);

    interface GroupRequestState {
        Long getGroupId();
        Long getRequestId();
        GroupJoinRequestStatus getStatus();
    }

    /** Latest per group, including the ID tie-break used by the single-group lookup. */
    @Query("""
        select r.studyGroup.id as groupId, r.id as requestId, r.status as status
        from GroupJoinRequest r
        where r.student.id=:studentId and r.studyGroup.id in :groupIds
          and not exists (
            select newer.id from GroupJoinRequest newer
            where newer.student.id=r.student.id and newer.studyGroup.id=r.studyGroup.id
              and (newer.createdAt>r.createdAt or (newer.createdAt=r.createdAt and newer.id>r.id))
          )
        """)
    List<GroupRequestState> findLatestForGroups(@Param("studentId") Long studentId, @Param("groupIds") List<Long> groupIds);
    List<GroupJoinRequest> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);
    List<GroupJoinRequest> findByStudyGroupIdAndStatus(Long studyGroupId, GroupJoinRequestStatus status);
    List<GroupJoinRequest> findByStudentId(Long studentId);
    List<GroupJoinRequest> findByStudyGroupId(Long groupId);
    List<GroupJoinRequest> findByStudentIdAndStatus(Long studentId, GroupJoinRequestStatus status);
    List<GroupJoinRequest> findByStudyGroupIdAndStatusOrderByCreatedAtDesc(Long studyGroupId,
        GroupJoinRequestStatus status);
    boolean existsByStudyGroupIdAndStudentIdAndStatus(Long studyGroupId, Long studentId,
        GroupJoinRequestStatus status);
}
