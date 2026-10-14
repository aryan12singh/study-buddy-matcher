package com.studybuddy.studysession;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.study-session")
public record SessionSettings(@DefaultValue("52") int maximumOccurrences,
    @DefaultValue("24") int maximumDurationHours, @DefaultValue("Asia/Singapore") String availabilityTimeZone) {
    public SessionSettings {
        if (maximumOccurrences < 1 || maximumOccurrences > 52 || maximumDurationHours < 1 || maximumDurationHours > 168
            || !"Asia/Singapore".equals(availabilityTimeZone)) {
            throw new IllegalArgumentException("Invalid study session configuration");
        }
    }
}
