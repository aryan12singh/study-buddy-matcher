package com.studybuddy.studysession;

import com.studybuddy.common.error.InvalidInputException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Each weekly occurrence preserves wall time; gaps and repeated local times require an explicit different time. */
public record SessionSchedule(LocalDateTime startLocal, LocalDateTime endLocal, String timeZone, int occurrenceCount) {
    public SessionSchedule {
        if (startLocal == null || endLocal == null) invalid("startLocal", "Start and end are required");
        if (timeZone == null || !ZoneId.getAvailableZoneIds().contains(timeZone)) invalid("timeZone", "Choose a valid IANA timezone");
        if (occurrenceCount < 1 || occurrenceCount > 52) invalid("occurrenceCount", "Use between 1 and 52 weekly occurrences");
        if (startLocal.getSecond() != 0 || startLocal.getNano() != 0 || endLocal.getSecond() != 0 || endLocal.getNano() != 0) {
            invalid("startLocal", "Session times must use whole minutes");
        }
        if (startLocal.getYear() < 1 || endLocal.getYear() > 9999) invalid("startLocal", "Choose a supported calendar date");
        if (!endLocal.isAfter(startLocal)) invalid("endLocal", "End must be after start");
        checkedOccurrences(startLocal, endLocal, timeZone, occurrenceCount);
    }

    public static SessionSchedule validate(SessionInput input, SessionSettings settings) {
        if (input.occurrenceCount() == null || input.occurrenceCount() > settings.maximumOccurrences()) {
            invalid("occurrenceCount", "Maximum weekly occurrences: " + settings.maximumOccurrences());
        }
        SessionSchedule result = new SessionSchedule(input.startLocal(), input.endLocal(), input.timeZone(), input.occurrenceCount());
        if (result.occurrences().stream().anyMatch(slot -> Duration.between(slot.startAt(), slot.endAt())
            .compareTo(Duration.ofHours(settings.maximumDurationHours())) > 0)) {
            invalid("endLocal", "A session can last at most " + settings.maximumDurationHours() + " hours");
        }
        return result;
    }

    public List<SessionOccurrenceDto> occurrences() {
        return checkedOccurrences(startLocal, endLocal, timeZone, occurrenceCount);
    }

    private static List<SessionOccurrenceDto> checkedOccurrences(LocalDateTime start, LocalDateTime end, String timeZone, int count) {
        ZoneId zone = ZoneId.of(timeZone);
        List<SessionOccurrenceDto> occurrences = new ArrayList<>();
        try {
            for (int index = 0; index < count; index++) {
                LocalDateTime localStart = start.plusWeeks(index);
                LocalDateTime localEnd = end.plusWeeks(index);
                if (localStart.getYear() > 9999 || localEnd.getYear() > 9999) invalid("occurrenceCount", "Series exceeds supported calendar dates");
                var starts = zone.getRules().getValidOffsets(localStart);
                var ends = zone.getRules().getValidOffsets(localEnd);
                if (starts.size() != 1) invalid("startLocal", "A weekly start falls in an ambiguous or missing time in this timezone");
                if (ends.size() != 1) invalid("endLocal", "A weekly end falls in an ambiguous or missing time in this timezone");
                var startAt = localStart.toInstant(starts.getFirst());
                var endAt = localEnd.toInstant(ends.getFirst());
                if (!endAt.isAfter(startAt)) invalid("endLocal", "Each occurrence must end after its start");
                occurrences.add(new SessionOccurrenceDto(startAt, endAt));
            }
        } catch (DateTimeException error) {
            invalid("occurrenceCount", "Series exceeds supported calendar dates");
        }
        return List.copyOf(occurrences);
    }

    private static void invalid(String field, String message) { throw new InvalidInputException(field, message); }
}
