package com.studybuddy.studysession;

import com.studybuddy.common.error.InvalidInputException;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionScheduleTest {
    private final SessionSettings settings = new SessionSettings(52, 24, "Asia/Singapore");
    @Test void weeklyDatesPreserveWallTimeAcrossDst() {
        var schedule = new SessionSchedule(LocalDateTime.parse("2026-10-25T10:00"), LocalDateTime.parse("2026-10-25T11:00"), "America/New_York", 3);
        assertEquals(Instant.parse("2026-10-25T14:00:00Z"), schedule.occurrences().get(0).startAt());
        assertEquals(Instant.parse("2026-11-01T15:00:00Z"), schedule.occurrences().get(1).startAt());
        assertEquals(Instant.parse("2026-11-08T15:00:00Z"), schedule.occurrences().get(2).startAt());
    }
    @Test void rejectsMissingAndAmbiguousTimesIncludingLaterWeeks() {
        assertThrows(InvalidInputException.class, () -> new SessionSchedule(LocalDateTime.parse("2026-03-01T02:30"), LocalDateTime.parse("2026-03-01T03:30"), "America/New_York", 2));
        assertThrows(InvalidInputException.class, () -> new SessionSchedule(LocalDateTime.parse("2026-11-01T01:30"), LocalDateTime.parse("2026-11-01T02:30"), "America/New_York", 1));
    }
    @Test void datedOvernightSessionWorksWhileDurationAndMinuteLimitsApply() {
        var overnight = SessionSchedule.validate(input("2026-11-01T23:30", "2026-11-02T00:30", "Asia/Singapore", 1), settings);
        assertEquals(3600, overnight.occurrences().getFirst().endAt().getEpochSecond() - overnight.occurrences().getFirst().startAt().getEpochSecond());
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T23:30", "2026-11-03T00:30", "Asia/Singapore", 1), settings));
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T23:30:01", "2026-11-02T00:30", "Asia/Singapore", 1), settings));
    }
    @Test void invalidZoneCountsAndOrderReturnFieldErrors() {
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T10:00", "2026-11-01T09:00", "Asia/Singapore", 1), settings));
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T10:00", "2026-11-01T11:00", "+08:00", 1), settings));
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T10:00", "2026-11-01T11:00", "Asia/Singapore", 53), settings));
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T10:00", "2026-11-01T11:00", "Asia/Singapore", 0), settings));
    }
    @Test void configurableLimitIsUsedRatherThanOnlyDatabaseMaximum() {
        assertThrows(InvalidInputException.class, () -> SessionSchedule.validate(input("2026-11-01T10:00", "2026-11-01T11:00", "Asia/Singapore", 3), new SessionSettings(2, 24, "Asia/Singapore")));
    }
    @Test void safeLinksAllowOnlyOwnRoomOrCredentialFreeHttps() {
        SessionInput original = input("2026-11-01T10:00", "2026-11-01T11:00", "Asia/Singapore", 1);
        assertEquals("/groups/3/room", details(original, "/groups/3/room", 3L).roomLink());
        assertEquals("https://meet.example.org/revision", details(original, "https://meet.example.org/revision", null).roomLink());
        for (String link : new String[]{"/groups/4/room", "javascript:alert(1)", "https://user:secret@meet.example.org/", "https://meet.example.org/?token=secret", "https://meet.example.org/#secret"}) {
            assertThrows(InvalidInputException.class, () -> details(original, link, 3L));
        }
    }
    private SessionDetails details(SessionInput input, String link, Long groupId) {
        return SessionDetails.validate(new SessionInput(input.title(), input.startLocal(), input.endLocal(), input.timeZone(), input.occurrenceCount(), null, null, link, null), settings, groupId);
    }
    private SessionInput input(String start, String end, String zone, int count) {
        return new SessionInput("Revision", LocalDateTime.parse(start), LocalDateTime.parse(end), zone, count, null, null, null, null);
    }
}
