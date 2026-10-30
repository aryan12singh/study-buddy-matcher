package com.studybuddy.studysession;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Queries study sessions: by id with a write lock, the group or connection a session belongs to, a
 * group's or connection's sessions, and every scheduled session a student can still attend.
 */
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    interface Parent { Long getGroupId(); Long getConnectionId(); }
    @Query("select s.group.id as groupId, s.connection.id as connectionId from StudySession s where s.id=:id")
    Optional<Parent> findParent(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudySession s where s.id=:id")
    Optional<StudySession> findByIdForUpdate(@Param("id") Long id);
    List<StudySession> findByGroupIdOrderByStartAtAsc(Long groupId);
    List<StudySession> findByConnectionIdOrderByStartAtAsc(Long connectionId);
    List<StudySession> findByGroupIdAndStatus(Long groupId, SessionStatus status);
    List<StudySession> findByConnectionIdAndStatus(Long connectionId, SessionStatus status);
    @Query("""
        select s from StudySession s left join s.group g left join s.connection c
        left join c.studentA a left join c.studentB b left join a.user au left join b.user bu
        where s.status=com.studybuddy.studysession.SessionStatus.SCHEDULED and
        ((g.active=true and g.id in (select m.studyGroup.id from GroupMembership m where m.student.id=:actorId))
        or (c.endedAt is null and au.active=true and bu.active=true
        and (a.id=:actorId or b.id=:actorId)))
        order by s.startAt, s.id
        """)
    List<StudySession> findMine(@Param("actorId") Long actorId);
    @Query("select s from StudySession s where s.status=com.studybuddy.studysession.SessionStatus.SCHEDULED and (s.connection.studentA.id=:studentId or s.connection.studentB.id=:studentId)")
    List<StudySession> findScheduledBuddyForStudent(@Param("studentId") Long studentId);
}
