package com.studybuddy.assistant;

import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.studygroup.StudyGroupSummaryDto;
import com.studybuddy.studysession.SessionDto;
import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.StudySessionService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Finds which of the student's upcoming study sessions a message means, from the session service's
 * own list: the group named (by name or course code), and the meeting day asked about. Soonest
 * first, so "our next session" is the first one.
 */
@Component
class AssistantSessions {
    /** "Monday 2 November", as the replies name a meeting day. */
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);

    private final StudySessionService sessions;
    private final StudyGroupService groups;
    private final Clock clock;

    AssistantSessions(StudySessionService sessions, StudyGroupService groups, Clock clock) {
        this.sessions = sessions;
        this.groups = groups;
        this.clock = clock;
    }

    /** An upcoming session with its group, or no group for a buddy session, and the meeting meant. */
    record Upcoming(SessionDto session, StudyGroupSummaryDto group, LocalDate meetingDay) {
        /** "Exam revision (IS442 Design Patterns Clinic)", or just the title for a buddy session. */
        String name() {
            return group == null ? session.title() : session.title() + " (" + group.name() + ")";
        }

        /** "Monday 2 November". */
        String day() {
            return DAY.format(meetingDay);
        }

        String link() {
            return "/sessions/" + session.id();
        }
    }

    /** The student's upcoming sessions the message could mean, soonest first. */
    List<Upcoming> meant(Long actorId, AssistantRequest request) {
        Map<Long, StudyGroupSummaryDto> groupsById = groups.mine(actorId).stream()
                .collect(Collectors.toMap(StudyGroupSummaryDto::id, Function.identity(), (first, second) -> first));
        String named = request.groupName() == null ? null : request.groupName().toLowerCase(Locale.ROOT);
        String code = request.courseCode() == null ? null : request.courseCode().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return sessions.mine(actorId).stream()
                .map(session -> {
                    StudyGroupSummaryDto group = session.groupId() == null ? null : groupsById.get(session.groupId());
                    return meetingDay(session, request.meetingDate()).map(day -> new Upcoming(session, group, day));
                })
                .flatMap(Optional::stream)
                .filter(upcoming -> named == null || upcoming.group() != null && upcoming.group().name().toLowerCase(Locale.ROOT).contains(named))
                .filter(upcoming -> code == null || upcoming.group() != null && code.equals(upcoming.group().courseCode()))
                .toList();
    }

    /** The meeting on the day asked about, or the next one; nothing when the session does not meet that day. */
    private Optional<LocalDate> meetingDay(SessionDto session, LocalDate asked) {
        ZoneId zone = ZoneId.of(session.timeZone());
        List<LocalDate> days = session.occurrences().stream()
                .filter(meeting -> meeting.endAt().isAfter(clock.instant()))
                .map(SessionOccurrenceDto::startAt)
                .map(start -> LocalDate.ofInstant(start, zone))
                .toList();
        if (asked == null) return days.stream().findFirst();
        return days.contains(asked) ? Optional.of(asked) : Optional.empty();
    }
}
