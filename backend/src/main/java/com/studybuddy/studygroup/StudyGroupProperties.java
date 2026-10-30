package com.studybuddy.studygroup;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Tunable study group limits, bound from {@code app.study-group.*}. */
@Validated
@ConfigurationProperties("app.study-group")
public record StudyGroupProperties(@Min(StudyGroupDetails.MIN_GROUP_SIZE) int maxCapacity) {
}
