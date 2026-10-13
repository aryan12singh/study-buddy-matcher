package com.studybuddy.student;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Tunable limits for the student profile, bound from {@code app.profile.*}. */
@Validated
@ConfigurationProperties("app.profile")
public record ProfileProperties(
        @Min(3) int groupSizeMax,
        @Min(1) int maxCourses) {
}
