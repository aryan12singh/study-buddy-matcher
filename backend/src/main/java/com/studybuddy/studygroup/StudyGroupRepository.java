package com.studybuddy.studygroup;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from StudyGroup g where g.id=:id")
    Optional<StudyGroup> findByIdForUpdate(@Param("id") Long id);

    @Query("select g.leader.id from StudyGroup g where g.id=:id")
    Optional<Long> findLeaderId(@Param("id") Long id);

    @Query("select g from StudyGroup g where g.leader.id=:studentId or g.id in (select m.studyGroup.id from GroupMembership m where m.student.id=:studentId) order by g.createdAt desc, g.id desc")
    List<StudyGroup> findMine(@Param("studentId") Long studentId);
    List<StudyGroup> findByLeaderId(Long leaderId);

    /** Open groups, newest first. Browse filters are applied by {@link StudyGroupFilter}. */
    List<StudyGroup> findByActiveTrueOrderByCreatedAtDesc();
    List<StudyGroup> findByLeaderIdAndActiveTrue(Long leaderId);

    /** Every group the student has led, open or closed. */
    long countByLeaderId(Long leaderId);
}
