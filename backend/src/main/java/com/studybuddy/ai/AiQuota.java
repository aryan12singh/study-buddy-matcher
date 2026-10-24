package com.studybuddy.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Decides whether the assistant may call Gemini now: within both the shared daily allowance and the
 * asking student's share of it, and not while pausing after Gemini said it was rate limited. Counts
 * separately from E1's explanation quota, because the two features use separate keys. Kept in
 * memory: a restart resets the count, which suits a single coursework instance.
 */
@Component
public class AiQuota {
    private final int dailyLimit;
    private final int dailyLimitPerStudent;
    private final Duration cooldown;
    private final Clock clock;
    private LocalDate day;
    private int used;
    private final Map<Long, Integer> usedByStudent = new HashMap<>();
    private Instant pausedUntil = Instant.MIN;

    /**
     * The day boundary follows the students' time zone, not the server's. The clock is built here
     * rather than as a shared bean, so it never competes with the application's UTC clock.
     */
    @Autowired
    public AiQuota(AiAssistantProperties properties) {
        this(properties, Clock.system(ZoneId.of(properties.quotaZone())));
    }

    AiQuota(AiAssistantProperties properties, Clock clock) {
        this.dailyLimit = properties.dailyCallLimit();
        this.dailyLimitPerStudent = properties.dailyCallLimitPerStudent();
        this.cooldown = properties.rateLimitCooldown();
        this.clock = clock;
    }

    /** One call for this student, within both their own and everyone's allowance for today. */
    public synchronized boolean tryAcquire(Long studentId) {
        if (clock.instant().isBefore(pausedUntil)) {
            return false;
        }
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
            usedByStudent.clear();
        }
        int usedByThisStudent = usedByStudent.getOrDefault(studentId, 0);
        if (used >= dailyLimit || usedByThisStudent >= dailyLimitPerStudent) {
            return false;
        }
        used++;
        usedByStudent.put(studentId, usedByThisStudent + 1);
        return true;
    }

    /** Gemini refused a call for being too frequent; stop asking until the cooldown passes. */
    public synchronized void pauseAfterRateLimit() {
        pausedUntil = clock.instant().plus(cooldown);
    }
}
