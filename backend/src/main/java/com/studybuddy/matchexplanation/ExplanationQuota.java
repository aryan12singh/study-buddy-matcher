package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

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

    public ExplanationQuota(AiExplanationProperties properties, Clock explanationQuotaClock) {
        this.dailyLimit = properties.dailyCallLimit();
        this.clock = explanationQuotaClock;
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
