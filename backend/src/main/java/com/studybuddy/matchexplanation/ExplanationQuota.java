package com.studybuddy.matchexplanation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Counts AI provider calls per day, so a busy demo falls back to the standard explanation
 * instead of exhausting the free tier. Kept in memory: a restart resets the count, which is
 * acceptable for a coursework deployment and documented in the README.
 */
@Component
public class ExplanationQuota {
    private final int dailyLimit;
    private final Clock clock;
    private LocalDate day;
    private int used;

    /**
     * The day boundary follows the students' time zone, not the server's. The clock is built here
     * rather than as a shared bean, so it never competes with the application's UTC clock.
     */
    @Autowired
    public ExplanationQuota(AiExplanationProperties properties) {
        this(properties, Clock.system(ZoneId.of(properties.quotaZone())));
    }

    ExplanationQuota(AiExplanationProperties properties, Clock clock) {
        this.dailyLimit = properties.dailyCallLimit();
        this.clock = clock;
    }

    public synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        if (used >= dailyLimit) {
            return false;
        }
        used++;
        return true;
    }
}
