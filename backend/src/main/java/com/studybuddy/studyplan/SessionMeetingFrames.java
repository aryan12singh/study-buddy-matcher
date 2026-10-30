package com.studybuddy.studyplan;

import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.SessionStatus;
import com.studybuddy.studysession.StudySession;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** A planned roadmap cut down to the meetings that have not ended yet. */
    public record StillUpcoming(RoadmapFrame frame, RoadmapDraft draft) { }

    /**
     * Leaves out of a roadmap the meetings that have ended since it was planned, renumbering the
     * rest from 1. Only for an unedited session (same version), whose remaining meeting dates still
     * match the plan's.
     *
     * @throws NoUpcomingMeetingsException when every planned meeting has ended
     */
    public StillUpcoming dropEnded(StudySession session, RoadmapFrame planned, RoadmapDraft draft) {
        Set<LocalDate> upcomingDates = upcoming(session).blocks().stream().map(RoadmapBlock::date).collect(Collectors.toSet());
        List<RoadmapBlock> kept = planned.blocks().stream().filter(block -> upcomingDates.contains(block.date())).toList();
        if (kept.isEmpty()) {
            throw new NoUpcomingMeetingsException(session.getId());
        }
        Map<Integer, Integer> newSequences = new HashMap<>();
        for (int index = 0; index < kept.size(); index++) {
            newSequences.put(kept.get(index).sequence(), index + 1);
        }
        RoadmapFrame frame = RoadmapFrame.meetings(kept.stream().map(RoadmapBlock::date).toList(), planned.minutesPerBlock());
        return new StillUpcoming(frame, draft.renumbered(newSequences));
    }
}
