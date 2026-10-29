package com.studybuddy.forest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FocusVisitRepository extends JpaRepository<FocusVisit, Long> {
    Optional<FocusVisit> findByStudentIdAndStudyGroupIdAndEndedAtIsNull(Long studentId, Long groupId);

    List<FocusVisit> findByStudentIdAndFocusedMillisGreaterThanEqualOrderByStartedAtDesc(Long studentId, long minimumMillis);

    /** Two tabs may open the same visit at once; the partial unique index lets only one row in. */
    @Modifying
    @Query(value = "insert into focus_visits(student_id,study_group_id,group_name,started_at,last_seen_at) "
        + "values (:studentId,:groupId,:groupName,:at,:at) "
        + "on conflict (student_id,study_group_id) where ended_at is null do nothing", nativeQuery = true)
    void open(@Param("studentId") Long studentId, @Param("groupId") Long groupId,
        @Param("groupName") String groupName, @Param("at") Instant at);

    /** Records a visit that has already finished, for the demo data. */
    @Modifying
    @Query(value = "insert into focus_visits(student_id,study_group_id,group_name,started_at,last_seen_at,focused_millis,ended_at) "
        + "values (:studentId,:groupId,:groupName,:startedAt,:endedAt,:millis,:endedAt)", nativeQuery = true)
    void insertFinished(@Param("studentId") Long studentId, @Param("groupId") Long groupId,
        @Param("groupName") String groupName, @Param("startedAt") Instant startedAt, @Param("endedAt") Instant endedAt,
        @Param("millis") long millis);

    /** Adds focus time only if no other tab has moved the visit on since {@code previous}. */
    @Modifying(clearAutomatically = true)
    @Query("update FocusVisit v set v.focusedMillis = v.focusedMillis + :millis, v.lastSeenAt = :now "
        + "where v.id = :id and v.lastSeenAt = :previous and v.endedAt is null")
    int credit(@Param("id") Long id, @Param("previous") Instant previous, @Param("now") Instant now,
        @Param("millis") long millis);

    @Modifying(clearAutomatically = true)
    @Query("update FocusVisit v set v.endedAt = :at where v.id = :id and v.endedAt is null")
    void end(@Param("id") Long id, @Param("at") Instant at);
}
