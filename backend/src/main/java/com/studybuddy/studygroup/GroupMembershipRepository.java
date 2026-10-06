package com.studybuddy.studygroup;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByStudyGroupId(Long studyGroupId);
    List<GroupMembership> findByStudentId(Long studentId);
    List<GroupMembership> findByStudyGroupIdOrderByJoinedAtAsc(Long studyGroupId);
    Optional<GroupMembership> findByStudyGroupIdAndStudentId(Long studyGroupId, Long studentId);
    boolean existsByStudyGroupIdAndStudentId(Long studyGroupId, Long studentId);

    /** Members of the group, leader included. */
    long countByStudyGroupId(Long studyGroupId);

    @Query("select count(m) from GroupMembership m where m.student.id=:studentId and m.studyGroup.active=true")
    long countAcceptedOpenByStudentId(@Param("studentId") Long studentId);

    /**
     * Groups the student is a member of but does not lead. The leader is
     * stored as a member too, so without this filter every group they lead
     * would also count as one they joined.
     */
    @Query("""
            select count(m) from GroupMembership m
            where m.student.id = :studentId
              and m.studyGroup.leader.id <> :studentId
            """)
    long countJoinedByStudentId(@Param("studentId") Long studentId);
}
