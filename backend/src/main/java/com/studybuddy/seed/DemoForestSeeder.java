package com.studybuddy.seed;

import com.studybuddy.forest.ForestProperties;
import com.studybuddy.forest.ForestService;
import com.studybuddy.seed.DemoData.ForestEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.studybuddy.seed.DemoSessionSeeder.groupIdOf;

/**
 * Plants the demo forest from finished study room visits dated relative to the seed run, so the
 * forest page and its tree tiers can be shown without studying for hours first.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoForestSeeder {

    private final ForestService forest;
    private final ForestProperties properties;
    private final Clock clock;

    public DemoForestSeeder(ForestService forest, ForestProperties properties, Clock clock) {
        this.forest = forest;
        this.properties = properties;
        this.clock = clock;
    }

    public void seed(DemoData data, DemoRoster roster, Map<String, Long> groupIds) {
        List<ForestEntry> entries = data.forest() == null ? List.of() : data.forest();
        LocalDate today = LocalDate.now(clock.withZone(properties.zone()));
        for (ForestEntry entry : entries) {
            var startedAt = today.minusDays(entry.daysAgo()).atTime(entry.startTime()).atZone(properties.zone()).toInstant();
            forest.recordFinishedVisit(roster.id(entry.student()), groupIdOf(entry.group(), groupIds), entry.group(),
                    startedAt, Duration.ofMinutes(entry.focusedMinutes()));
        }
    }
}
