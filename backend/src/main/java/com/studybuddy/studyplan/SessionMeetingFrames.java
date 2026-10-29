package com.studybuddy.studyplan;

import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.SessionStatus;
import com.studybuddy.studysession.StudySession;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Decides which meetings a session plan covers: the session's meetings that have not yet ended,
 * from the one under way or next, up to the configured number. Each meeting's date is its day in
 * the session's own time zone, and every meeting holds the shortest of their lengths, so a clock
 * change in the series never lets an agenda overrun.
 */
@Component
public class SessionMeetingFrames {
    private final StudyPlanProperties properties;
    private final Clock clock;

    public SessionMeetingFrames(StudyPlanProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** @throws NoUpcomingMeetingsException when the session is cancelled or all its meetings have ended */
    public RoadmapFrame upcoming(StudySession session) {
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new NoUpcomingMeetingsException(session.getId());
        }
        List<SessionOccurrenceDto> meetings = session.schedule().occurrences().stream()
                .filter(meeting -> meeting.endAt().isAfter(clock.instant()))
                .limit(properties.maxSessionMeetings())
                .toList();
        if (meetings.isEmpty()) {
            throw new NoUpcomingMeetingsException(session.getId());
        }
        ZoneId zone = ZoneId.of(session.getTimeZone());
        List<LocalDate> dates = meetings.stream().map(meeting -> LocalDate.ofInstant(meeting.startAt(), zone)).toList();
        int minutes = meetings.stream()
                .mapToInt(meeting -> (int) Duration.between(meeting.startAt(), meeting.endAt()).toMinutes())
                .min().orElseThrow();
        return RoadmapFrame.meetings(dates, minutes);
    }
}
