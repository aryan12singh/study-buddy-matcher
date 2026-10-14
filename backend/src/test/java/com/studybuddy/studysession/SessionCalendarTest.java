package com.studybuddy.studysession;

import com.studybuddy.studygroup.StudyGroup;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionCalendarTest {
    @Test void exportsPrivateExplicitEventsWithCorrectDstInstantsAndStableRevisions() {
        StudySession session = session("Revision", "Plan", "Library");
        String before = new SessionCalendar().export(session).content();
        assertEquals(3, before.split("BEGIN:VEVENT", -1).length - 1);
        assertTrue(before.contains("DTSTART:20261025T140000Z\r\n"));
        assertTrue(before.contains("DTSTART:20261101T150000Z\r\n"));
        assertFalse(before.contains("RRULE"));
        assertFalse(before.contains("ATTENDEE"));
        assertFalse(before.contains("ORGANIZER"));
        assertTrue(before.contains("CLASS:PRIVATE\r\n"));
        session.cancel(Instant.parse("2026-10-15T00:00:00Z"));
        String cancelled = new SessionCalendar().export(session).content();
        assertEquals(Arrays.stream(before.split("\r\n")).filter(line -> line.startsWith("UID:")).toList(),
            Arrays.stream(cancelled.split("\r\n")).filter(line -> line.startsWith("UID:")).toList());
        assertEquals(3, cancelled.split("STATUS:CANCELLED", -1).length - 1);
    }
    @Test void escapesUntrustedTextAndFoldsUtf8WithoutSplittingCodePoints() {
        StudySession session = session("界".repeat(50) + ",;\\", "First\r\nBEGIN:VEVENT\r\nSecond", "Room;2");
        String content = new SessionCalendar().export(session).content();
        for (String line : content.split("\r\n")) assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, line);
        String unfolded = content.replace("\r\n ", "");
        assertTrue(unfolded.contains("SUMMARY:" + "界".repeat(50) + "\\,\\;\\\\\r\n"));
        assertTrue(unfolded.contains("DESCRIPTION:First\\nBEGIN:VEVENT\\nSecond\r\n"));
        assertTrue(unfolded.contains("LOCATION:Room\\;2\r\n"));
        assertEquals(3, content.split("\r\nBEGIN:VEVENT\r\n", -1).length - 1);
        assertFalse(content.contains("\uFFFD"));
        assertFalse(content.replace("\r\n", "").contains("\n"));
    }
    private StudySession session(String title, String agenda, String location) {
        return new StudySession(new StudyGroup("Revision group", null, null, null, 4), null, 1L,
            new SessionDetails(title, new SessionSchedule(LocalDateTime.parse("2026-10-25T10:00"), LocalDateTime.parse("2026-10-25T11:00"), "America/New_York", 3), agenda, location, null),
            Instant.parse("2026-10-14T00:00:00Z"));
    }
}
