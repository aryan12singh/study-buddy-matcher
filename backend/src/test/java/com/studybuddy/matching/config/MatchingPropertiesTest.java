package com.studybuddy.matching.config;

import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchingPropertiesTest {

    private static Map<String, String> validSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("app.matching.default-strategy", "COURSE_FIRST");
        settings.put("app.matching.default-weights.balanced.course", "0.3");
        settings.put("app.matching.default-weights.balanced.availability", "0.3");
        settings.put("app.matching.default-weights.balanced.study-mode", "0.2");
        settings.put("app.matching.default-weights.balanced.study-goal", "0.1");
        settings.put("app.matching.default-weights.balanced.group-size", "0.1");
        settings.put("app.matching.default-weights.availability-first.course", "0.4");
        settings.put("app.matching.default-weights.availability-first.study-mode", "0.3");
        settings.put("app.matching.default-weights.availability-first.study-goal", "0.15");
        settings.put("app.matching.default-weights.availability-first.group-size", "0.15");
        settings.put("app.matching.default-weights.course-first.availability", "0.4");
        settings.put("app.matching.default-weights.course-first.study-mode", "0.3");
        settings.put("app.matching.default-weights.course-first.study-goal", "0.15");
        settings.put("app.matching.default-weights.course-first.group-size", "0.15");
        settings.put("app.matching.quality.strong", "0.75");
        settings.put("app.matching.quality.good", "0.5");
        return settings;
    }

    private static MatchingProperties bind(Map<String, String> settings) {
        return new Binder(new MapConfigurationPropertySource(settings))
                .bind("app.matching", MatchingProperties.class)
                .get();
    }

    @Test
    void bindsKebabCaseStrategyAndCriterionKeys() {
        MatchingProperties properties = bind(validSettings());

        assertEquals(MatchingStrategyType.COURSE_FIRST, properties.defaultStrategy());
        assertEquals(0.2, properties.weightsFor(MatchingStrategyType.BALANCED).weightOf(MatchingCriterion.STUDY_MODE));
        assertEquals(0.4, properties.weightsFor(MatchingStrategyType.AVAILABILITY_FIRST).weightOf(MatchingCriterion.COURSE));
        assertEquals(MatchQuality.GOOD, properties.quality().classify(0.6));
    }

    @Test
    void aCriterionLeftOutOfAStrategyCountsAsZero() {
        MatchingProperties properties = bind(validSettings());

        assertEquals(0.0, properties.weightsFor(MatchingStrategyType.AVAILABILITY_FIRST).weightOf(MatchingCriterion.AVAILABILITY));
        assertEquals(0.0, properties.weightsFor(MatchingStrategyType.COURSE_FIRST).weightOf(MatchingCriterion.COURSE));
    }

    @Test
    void defaultsThatDoNotAddUpToOneFailToBind() {
        Map<String, String> settings = validSettings();
        settings.remove("app.matching.default-weights.balanced.study-goal");

        assertThrows(BindException.class, () -> bind(settings));
    }

    @Test
    void aStrategyWithNoDefaultsFailsToBind() {
        Map<String, String> settings = validSettings();
        settings.keySet().removeIf(key -> key.contains(".course-first."));

        assertThrows(BindException.class, () -> bind(settings));
    }
}
