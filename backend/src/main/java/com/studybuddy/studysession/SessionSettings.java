package com.studybuddy.studysession;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Study session limits bound from {@code app.study-session}: the most weekly occurrences one
 * session may have and the longest a single occurrence may last.
 */
@ConfigurationProperties("app.study-session")
public record SessionSettings(@DefaultValue("52") int maximumOccurrences, @DefaultValue("24") int maximumDurationHours) {
    public SessionSettings {
        if (maximumOccurrences < 1 || maximumOccurrences > SessionSchedule.MAX_OCCURRENCES
            || maximumDurationHours < 1 || maximumDurationHours > 168) {
            throw new IllegalArgumentException("Invalid study session configuration");
        }
    }
}
