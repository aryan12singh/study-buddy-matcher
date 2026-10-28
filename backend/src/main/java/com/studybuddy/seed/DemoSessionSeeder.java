package com.studybuddy.seed;

import com.studybuddy.connection.ConnectionDto;
import com.studybuddy.connection.ConnectionService;
import com.studybuddy.seed.DemoData.SessionEntry;
import com.studybuddy.seed.DemoData.TimeBlock;
import com.studybuddy.studysession.SessionInput;
import com.studybuddy.studysession.StudySessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Schedules the demo's group and buddy sessions through the session service, each starting at its
 * time block's next occurrence after the seed runs, so the sessions are always upcoming.
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

    public void seed(DemoData data, DemoRoster roster, Map<String, Long> groupIds) {
        for (SessionEntry entry : data.sessions()) {
            Long organiser = roster.id(entry.organiser());
            SessionInput input = inputFor(entry, data.block(entry.block()));
            if (entry.group() != null) {
                sessions.createGroup(groupIdOf(entry.group(), groupIds), organiser, input);
            } else {
                sessions.createBuddy(connectionBetween(organiser, roster.id(entry.buddy())), organiser, input);
            }
        }
    }

    private SessionInput inputFor(SessionEntry entry, TimeBlock block) {
        LocalDateTime start = block.nextStartAfter(LocalDateTime.now(clock.withZone(ZoneId.of(entry.timeZone()))));
        return new SessionInput(entry.title(), start, start.with(block.end()), entry.timeZone(), entry.weeks(),
                entry.agenda(), entry.location(), null, null);
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
