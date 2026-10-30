package com.studybuddy.student;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Weekly availability limits shared by student profiles and study groups, bound from
 * {@code app.availability.*}. The cap keeps one save, and the matching maths over it, bounded.
 */
@Validated
@ConfigurationProperties("app.availability")
public record AvailabilityProperties(@Min(1) int maxWeeklySlots) {
}
