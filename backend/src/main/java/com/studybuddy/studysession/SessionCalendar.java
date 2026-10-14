package com.studybuddy.studysession;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** RFC 5545 snapshot: explicit UTC events preserve bounded weekly wall times across DST without client-specific timezone rules. */
@Component
public class SessionCalendar {
    private static final int CONTENT_LINE_OCTETS = 75;
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    public CalendarExportDto export(StudySession session) {
        StringBuilder calendar = new StringBuilder();
        line(calendar, "BEGIN:VCALENDAR");
        line(calendar, "VERSION:2.0");
        line(calendar, "PRODID:-//Study Buddy Matcher//Study Sessions//EN");
        line(calendar, "CALSCALE:GREGORIAN");
        var occurrences = session.schedule().occurrences();
        for (int index = 0; index < occurrences.size(); index++) {
            var occurrence = occurrences.get(index);
            line(calendar, "BEGIN:VEVENT");
            line(calendar, "UID:" + session.getCalendarUid() + "-" + index + "@studybuddy.local");
            line(calendar, "DTSTAMP:" + UTC.format(session.getUpdatedAt()));
            line(calendar, "LAST-MODIFIED:" + UTC.format(session.getUpdatedAt()));
            line(calendar, "SEQUENCE:" + session.getVersion());
            line(calendar, "DTSTART:" + UTC.format(occurrence.startAt()));
            line(calendar, "DTEND:" + UTC.format(occurrence.endAt()));
            line(calendar, "SUMMARY:" + escape(session.getTitle()));
            if (session.getAgenda() != null) line(calendar, "DESCRIPTION:" + escape(session.getAgenda()));
            if (session.getLocation() != null) line(calendar, "LOCATION:" + escape(session.getLocation()));
            if (session.getRoomLink() != null && session.getRoomLink().regionMatches(true, 0, "https://", 0, 8)) line(calendar, "URL:" + session.getRoomLink());
            line(calendar, "STATUS:" + (session.getStatus() == SessionStatus.CANCELLED ? "CANCELLED" : "CONFIRMED"));
            line(calendar, "CLASS:PRIVATE");
            line(calendar, "END:VEVENT");
        }
        line(calendar, "END:VCALENDAR");
        return new CalendarExportDto("study-session-" + session.getId() + ".ics", calendar.toString());
    }
    static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\r\n", "\n").replace("\r", "\n")
            .replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,");
    }
    /** Fold between code points, with the continuation space included in the 75-octet budget. */
    static void line(StringBuilder target, String value) {
        int octets = 0;
        for (int offset = 0; offset < value.length();) {
            int point = value.codePointAt(offset);
            String character = new String(Character.toChars(point));
            int bytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (octets + bytes > CONTENT_LINE_OCTETS) { target.append("\r\n "); octets = 1; }
            target.append(character);
            octets += bytes;
            offset += Character.charCount(point);
        }
        target.append("\r\n");
    }
}
