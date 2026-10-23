package com.studybuddy.achievement;

import com.studybuddy.studysession.StudySession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only schedules with the start of the student's current participation, fetched together. */
public interface AchievementSessionRepository extends Repository<StudySession, Long> {
    interface PlannedSchedule {
        LocalDateTime getStartLocal();
        LocalDateTime getEndLocal();
        String getTimeZone();
        int getOccurrenceCount();
        Instant getParticipationStartedAt();
    }

    @Query("""
            select s.startLocal as startLocal, s.endLocal as endLocal, s.timeZone as timeZone,
                   s.occurrenceCount as occurrenceCount,
                   coalesce(m.joinedAt, c.createdAt) as participationStartedAt
            from StudySession s
            left join s.group g
            left join GroupMembership m on m.studyGroup = g and m.student.id = :actorId
            left join s.connection c
            left join c.studentA a left join c.studentB b
            left join a.user au left join b.user bu
            where s.status = com.studybuddy.studysession.SessionStatus.SCHEDULED
              and ((g.active = true and m.id is not null)
                   or (c.endedAt is null and au.active = true and bu.active = true
                       and (a.id = :actorId or b.id = :actorId)))
            """)
    List<PlannedSchedule> findEligibleSchedules(@Param("actorId") Long actorId);
}
