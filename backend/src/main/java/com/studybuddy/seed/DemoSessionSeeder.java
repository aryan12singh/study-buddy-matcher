package com.studybuddy.seed;

import com.studybuddy.connection.ConnectionDto;
import com.studybuddy.connection.ConnectionService;
import com.studybuddy.seed.DemoData.SessionEntry;
import com.studybuddy.seed.DemoData.TimeBlock;
import com.studybuddy.studysession.SessionDto;
import com.studybuddy.studysession.SessionInput;
import com.studybuddy.studysession.StudySessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schedules the demo's group and buddy sessions through the session service, each starting at its
 * time block's next occurrence after the seed runs, so the sessions are always upcoming. A series
 * already under way starts the given number of weeks earlier; the service accepts any start, as a
 * student may also record a series that has already begun.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoSessionSeeder {

    private final StudySessionService sessions;
    private final ConnectionService connections;
    private final Clock clock;

    public DemoSessionSeeder(StudySessionService sessions, ConnectionService connections, Clock clock) {
        this.sessions = sessions;
        this.connections = connections;
        this.clock = clock;
    }

    /** Schedules the sessions and returns their ids by title, for the quizzes and roadmaps that follow. */
    public Map<String, Long> seed(DemoData data, DemoRoster roster, Map<String, Long> groupIds) {
        Map<String, Long> sessionIds = new LinkedHashMap<>();
        for (SessionEntry entry : data.sessions()) {
            Long organiser = roster.id(entry.organiser());
            SessionInput input = inputFor(entry, data.block(entry.block()));
            SessionDto session = entry.group() != null
                    ? sessions.createGroup(groupIdOf(entry.group(), groupIds), organiser, input)
                    : sessions.createBuddy(connectionBetween(organiser, roster.id(entry.buddy())), organiser, input);
            sessionIds.put(entry.title(), session.id());
        }
        return sessionIds;
    }

    private SessionInput inputFor(SessionEntry entry, TimeBlock block) {
        LocalDateTime start = block.nextStartAfter(LocalDateTime.now(clock.withZone(ZoneId.of(entry.timeZone()))))
                .minusWeeks(entry.weeksAgo());
        return new SessionInput(entry.title(), start, start.with(block.end()), entry.timeZone(), entry.weeks(),
                entry.agenda(), entry.location(), null, null);
    }

    static Long sessionIdOf(String title, Map<String, Long> sessionIds) {
        Long id = sessionIds.get(title);
        if (id == null) {
            throw new IllegalStateException("Demo data refers to unknown session " + title);
        }
        return id;
    }

    static Long groupIdOf(String name, Map<String, Long> groupIds) {
        Long id = groupIds.get(name);
        if (id == null) {
            throw new IllegalStateException("Demo data refers to unknown group " + name);
        }
        return id;
    }

    private Long connectionBetween(Long studentId, Long otherStudentId) {
        return connections.listActive(studentId).stream()
                .filter(connection -> connection.otherStudentId().equals(otherStudentId))
                .map(ConnectionDto::id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo session needs an active connection"));
    }
}
