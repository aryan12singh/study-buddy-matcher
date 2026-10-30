package com.studybuddy.studyplan;

import com.studybuddy.studysession.SessionSchedule;
import com.studybuddy.studysession.SessionStatus;
import com.studybuddy.studysession.StudySession;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionMeetingFramesTest {
    /** At most 3 meetings per plan, so the cap is easy to cross. */
    private static final StudyPlanProperties LIMITS =
            new StudyPlanProperties(DataSize.ofMegabytes(10), 6, 4000, 100000, 40, 5, 5, 16, 40, 3, "Asia/Singapore");

    /** Mondays 10:00 to 11:30 in Singapore from 2 November 2026, for the given number of weeks. */
    private static StudySession weekly(int weeks, SessionStatus status) {
        StudySession session = mock(StudySession.class);
        when(session.getId()).thenReturn(5L);
        when(session.getStatus()).thenReturn(status);
        when(session.getTimeZone()).thenReturn("Asia/Singapore");
        when(session.schedule()).thenReturn(new SessionSchedule(LocalDateTime.parse("2026-11-02T10:00"),
                LocalDateTime.parse("2026-11-02T11:30"), "Asia/Singapore", weeks));
        return session;
    }

    private static RoadmapFrame frameAt(String instant, StudySession session) {
        return new SessionMeetingFrames(LIMITS, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)).upcoming(session);
    }

    @Test
    void plansEachUpcomingMeetingOnItsOwnDateWithTheMeetingsLength() {
        RoadmapFrame frame = frameAt("2026-10-30T00:00:00Z", weekly(2, SessionStatus.SCHEDULED));

        assertEquals(RoadmapFrame.BlockKind.MEETING, frame.kind());
        assertEquals(List.of(new RoadmapBlock(1, LocalDate.of(2026, 11, 2), 90), new RoadmapBlock(2, LocalDate.of(2026, 11, 9), 90)),
                frame.blocks());
    }

    @Test
    void skipsMeetingsThatHaveEndedButKeepsTheOneUnderWay() {
        // 10:30 Singapore time on 9 November: the first meeting is over, the second is under way.
        RoadmapFrame frame = frameAt("2026-11-09T02:30:00Z", weekly(3, SessionStatus.SCHEDULED));

        assertEquals(List.of(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16)),
                frame.blocks().stream().map(RoadmapBlock::date).toList());
        assertEquals(1, frame.blocks().getFirst().sequence());
    }

    @Test
    void coversAtMostTheConfiguredNumberOfMeetings() {
        assertEquals(3, frameAt("2026-10-30T00:00:00Z", weekly(10, SessionStatus.SCHEDULED)).blocks().size());
    }

    @Test
    void aOneOffSessionIsASingleMeetingAgenda() {
        assertEquals(1, frameAt("2026-10-30T00:00:00Z", weekly(1, SessionStatus.SCHEDULED)).blocks().size());
    }

    @Test
    void refusesACancelledSessionOrOneWhoseMeetingsHaveAllEnded() {
        assertThrows(NoUpcomingMeetingsException.class, () -> frameAt("2026-10-30T00:00:00Z", weekly(2, SessionStatus.CANCELLED)));
        assertThrows(NoUpcomingMeetingsException.class, () -> frameAt("2026-11-09T03:30:00Z", weekly(2, SessionStatus.SCHEDULED)));
    }

    private static RoadmapDraft draftFor(int meetings) {
        List<RoadmapDraft.Entry> entries = new java.util.ArrayList<>();
        for (int sequence = 1; sequence <= meetings; sequence++) {
            entries.add(new RoadmapDraft.Entry(sequence, "Meeting " + sequence, 60, List.of("Task")));
        }
        return new RoadmapDraft(List.of("Topic"), entries);
    }

    @Test
    void dropsMeetingsThatEndedSincePlanningAndRenumbersTheRest() {
        StudySession session = weekly(3, SessionStatus.SCHEDULED);
        RoadmapFrame planned = frameAt("2026-10-30T00:00:00Z", session);
        // 12:00 Singapore time on 2 November: the first planned meeting has just ended.
        SessionMeetingFrames later = new SessionMeetingFrames(LIMITS, Clock.fixed(Instant.parse("2026-11-02T04:00:00Z"), ZoneOffset.UTC));

        SessionMeetingFrames.StillUpcoming kept = later.dropEnded(session, planned, draftFor(3));

        assertEquals(List.of(new RoadmapBlock(1, LocalDate.of(2026, 11, 9), 90), new RoadmapBlock(2, LocalDate.of(2026, 11, 16), 90)),
                kept.frame().blocks());
        assertEquals(List.of(1, 2), kept.draft().entries().stream().map(RoadmapDraft.Entry::sequence).toList());
        assertEquals(List.of("Meeting 2", "Meeting 3"), kept.draft().entries().stream().map(RoadmapDraft.Entry::title).toList());
    }

    @Test
    void keepsTheWholeRoadmapWhenNoMeetingHasEnded() {
        StudySession session = weekly(2, SessionStatus.SCHEDULED);
        RoadmapFrame planned = frameAt("2026-10-30T00:00:00Z", session);
        RoadmapDraft draft = draftFor(2);

        SessionMeetingFrames.StillUpcoming kept = new SessionMeetingFrames(LIMITS,
                Clock.fixed(Instant.parse("2026-10-30T00:01:00Z"), ZoneOffset.UTC)).dropEnded(session, planned, draft);

        assertEquals(planned, kept.frame());
        assertEquals(draft, kept.draft());
    }

    @Test
    void refusesWhenEveryPlannedMeetingHasEnded() {
        StudySession session = weekly(1, SessionStatus.SCHEDULED);
        RoadmapFrame planned = frameAt("2026-10-30T00:00:00Z", session);
        SessionMeetingFrames later = new SessionMeetingFrames(LIMITS, Clock.fixed(Instant.parse("2026-11-02T04:00:00Z"), ZoneOffset.UTC));

        assertThrows(NoUpcomingMeetingsException.class, () -> later.dropEnded(session, planned, draftFor(1)));
    }
}
