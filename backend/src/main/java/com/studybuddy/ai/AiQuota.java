package com.studybuddy.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Decides whether one AI client may call its provider now: within both the shared daily allowance
 * and the asking student's share of it, and not while pausing after the provider said it was rate
 * limited. Each client gets its own instance (study tools, assistant chat and E1 explanations use
 * separate keys), so one feature can never spend another's allowance. Kept in memory: a restart
 * resets the count, which suits a single coursework instance.
 */
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
    public AiQuota(int dailyLimit, int dailyLimitPerStudent, Duration cooldown, ZoneId zone) {
        this(dailyLimit, dailyLimitPerStudent, cooldown, Clock.system(zone));
    }

    AiQuota(int dailyLimit, int dailyLimitPerStudent, Duration cooldown, Clock clock) {
        this.dailyLimit = dailyLimit;
        this.dailyLimitPerStudent = dailyLimitPerStudent;
        this.cooldown = cooldown;
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

    /** The provider refused a call for being too frequent; stop asking until the cooldown passes. */
    public synchronized void pauseAfterRateLimit() {
        pausedUntil = clock.instant().plus(cooldown);
    }
}
