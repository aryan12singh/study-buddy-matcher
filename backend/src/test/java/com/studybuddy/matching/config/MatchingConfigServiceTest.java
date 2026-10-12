package com.studybuddy.matching.config;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.matching.MatchingFixtures;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.AvailabilityFirstStrategy;
import com.studybuddy.matching.strategy.BalancedStrategy;
import com.studybuddy.matching.strategy.CourseFirstStrategy;
import com.studybuddy.matching.strategy.MatchingStrategy;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.studybuddy.matching.MatchingFixtures.evenWeights;
import static com.studybuddy.matching.MatchingFixtures.strategyDefaults;
import static com.studybuddy.matching.strategy.MatchingStrategyType.AVAILABILITY_FIRST;
import static com.studybuddy.matching.strategy.MatchingStrategyType.BALANCED;
import static com.studybuddy.matching.strategy.MatchingStrategyType.COURSE_FIRST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingConfigServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final List<MatchingStrategy> STRATEGIES =
            List.of(new BalancedStrategy(), new AvailabilityFirstStrategy(), new CourseFirstStrategy());

    @Mock private MatchingConfigRepository weightRows;
    @Mock private MatchingStrategySettingRepository strategyRows;
    @Mock private AccountAccess access;

    private MatchingConfigService service;

    @BeforeEach
    void setUp() {
        service = new MatchingConfigService(weightRows, strategyRows, MatchingFixtures.properties(), STRATEGIES,
                new MatchingConfigAssembler(), access);
    }

    @Test
    void usesTheConfiguredDefaultsWhenNothingIsSaved() {
        when(weightRows.findAll()).thenReturn(List.of());
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        MatchingConfigDto config = service.get(ADMIN_ID);

        assertEquals(BALANCED, config.activeStrategy());
        assertEquals(strategyDefaults(), config.weights());
        verify(access).requireAdmin(ADMIN_ID);
    }

    @Test
    void eachStrategyShowsOnlyTheCriteriaItWeights() {
        when(weightRows.findAll()).thenReturn(List.of());
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        MatchingConfigDto config = service.get(ADMIN_ID);

        assertEquals(5, config.weights().get(BALANCED).size());
        assertFalse(config.weights().get(AVAILABILITY_FIRST).containsKey(MatchingCriterion.AVAILABILITY));
        assertFalse(config.weights().get(COURSE_FIRST).containsKey(MatchingCriterion.COURSE));
    }

    @Test
    void scoresWithTheActiveStrategysOwnSavedWeights() {
        when(weightRows.findAll()).thenReturn(List.of(
                new MatchingConfig(BALANCED, MatchingCriterion.COURSE, 0.9),
                new MatchingConfig(COURSE_FIRST, MatchingCriterion.AVAILABILITY, 0.7),
                new MatchingConfig(COURSE_FIRST, MatchingCriterion.STUDY_MODE, 0.1),
                new MatchingConfig(COURSE_FIRST, MatchingCriterion.STUDY_GOAL, 0.1),
                new MatchingConfig(COURSE_FIRST, MatchingCriterion.GROUP_SIZE, 0.1)));
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.of(new MatchingStrategySetting(COURSE_FIRST)));

        assertEquals(0.7, service.currentWeights().weightOf(MatchingCriterion.AVAILABILITY), 1e-9);
        assertEquals(0.0, service.currentWeights().weightOf(MatchingCriterion.COURSE));
        assertInstanceOf(CourseFirstStrategy.class, service.activeStrategy());
    }

    @Test
    void rescalesWeightsSavedBeforeTheyHadToAddUpToOne() {
        when(weightRows.findAll()).thenReturn(List.of(new MatchingConfig(BALANCED, MatchingCriterion.COURSE, 0.9)));
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        // 0.9 + 0.2 * 4 = 1.7: shares of 52.9 and 11.8 hundredths, kept to two decimal places.
        assertEquals(0.53, service.currentWeights().weightOf(MatchingCriterion.COURSE));
        assertEquals(0.12, service.currentWeights().weightOf(MatchingCriterion.AVAILABILITY));
        assertEquals(0.11, service.currentWeights().weightOf(MatchingCriterion.GROUP_SIZE));
    }

    @Test
    void updateSavesEveryStrategysWeightsAndTheActiveStrategy() {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights = strategyDefaults();
        weights.get(AVAILABILITY_FIRST).put(MatchingCriterion.COURSE, 0.4);
        weights.get(AVAILABILITY_FIRST).put(MatchingCriterion.GROUP_SIZE, 0.1);
        when(weightRows.findByStrategyAndCriterion(any(), any())).thenReturn(Optional.empty());
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        MatchingConfigDto saved = service.update(new UpdateMatchingConfigRequest(AVAILABILITY_FIRST, weights), ADMIN_ID);

        assertEquals(AVAILABILITY_FIRST, saved.activeStrategy());
        assertEquals(0.4, saved.weights().get(AVAILABILITY_FIRST).get(MatchingCriterion.COURSE));
        verify(access).lockAdmin(ADMIN_ID);
        verify(weightRows, times(MatchingStrategyType.values().length * MatchingCriterion.values().length)).save(any());
        ArgumentCaptor<MatchingStrategySetting> strategy = ArgumentCaptor.forClass(MatchingStrategySetting.class);
        verify(strategyRows).save(strategy.capture());
        assertEquals(AVAILABILITY_FIRST, strategy.getValue().getActiveStrategy());
    }

    @Test
    void updateChangesExistingRowsInsteadOfAddingNewOnes() {
        MatchingConfig existing = new MatchingConfig(BALANCED, MatchingCriterion.COURSE, 0.1);
        MatchingStrategySetting existingStrategy = new MatchingStrategySetting(BALANCED);
        when(weightRows.findByStrategyAndCriterion(any(), any())).thenReturn(Optional.empty());
        when(weightRows.findByStrategyAndCriterion(BALANCED, MatchingCriterion.COURSE)).thenReturn(Optional.of(existing));
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existingStrategy));

        service.update(new UpdateMatchingConfigRequest(COURSE_FIRST, strategyDefaults()), ADMIN_ID);

        assertEquals(0.2, existing.getWeight());
        assertEquals(COURSE_FIRST, existingStrategy.getActiveStrategy());
        verify(weightRows).save(existing);
        verify(strategyRows).save(existingStrategy);
    }

    @Test
    void theCriterionAStrategyRanksByFirstIsSavedAsZero() {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights = strategyDefaults();
        weights.get(COURSE_FIRST).put(MatchingCriterion.COURSE, 0.0);
        when(weightRows.findByStrategyAndCriterion(any(), any())).thenReturn(Optional.empty());
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        ArgumentCaptor<MatchingConfig> rows = ArgumentCaptor.forClass(MatchingConfig.class);

        service.update(new UpdateMatchingConfigRequest(BALANCED, weights), ADMIN_ID);

        verify(weightRows, times(15)).save(rows.capture());
        MatchingConfig courseFirstCourse = rows.getAllValues().stream()
                .filter(row -> row.getStrategy() == COURSE_FIRST && row.getCriterion() == MatchingCriterion.COURSE)
                .findFirst().orElseThrow();
        assertEquals(0.0, courseFirstCourse.getWeight());
    }

    @Test
    void rejectsAWeightOnTheCriterionAStrategyRanksByFirst() {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights = strategyDefaults();
        weights.get(COURSE_FIRST).put(MatchingCriterion.COURSE, 0.2);

        InvalidMatchingConfigException error = assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, weights), ADMIN_ID));
        assertEquals("Course first already ranks by COURSE, so it cannot also weight it", error.getMessage());
        verify(weightRows, never()).save(any());
    }

    @Test
    void rejectsWeightsThatDoNotAddUpToOneNamingTheStrategy() {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights = strategyDefaults();
        weights.get(AVAILABILITY_FIRST).put(MatchingCriterion.COURSE, 10.0);

        InvalidMatchingConfigException error = assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, weights), ADMIN_ID));
        assertEquals("Availability first: The weights must add up to 1, but add up to 10.75", error.getMessage());
        verify(weightRows, never()).save(any());
        verify(strategyRows, never()).save(any());
    }

    @Test
    void rejectsAnUpdateMissingAStrategysWeights() {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights = strategyDefaults();
        weights.remove(COURSE_FIRST);

        InvalidMatchingConfigException error = assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, weights), ADMIN_ID));
        assertEquals("Weights for Course first are required", error.getMessage());
        assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, null), ADMIN_ID));
    }

    @Test
    void rejectsAnUpdateWithNoStrategy() {
        assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(null, strategyDefaults()), ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(null, ADMIN_ID));
    }

    @Test
    void nonAdminCannotReadOrChangeTheSettings() {
        when(access.requireAdmin(ADMIN_ID)).thenThrow(new ForbiddenActionException("An administrator account is required"));
        when(access.lockAdmin(ADMIN_ID)).thenThrow(new ForbiddenActionException("An administrator account is required"));

        assertThrows(ForbiddenActionException.class, () -> service.get(ADMIN_ID));
        assertThrows(ForbiddenActionException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, strategyDefaults()), ADMIN_ID));
        verify(weightRows, never()).save(any());
    }

    @Test
    void failsAtStartupIfAStrategyTypeHasNoImplementation() {
        assertThrows(IllegalStateException.class, () -> new MatchingConfigService(weightRows, strategyRows,
                MatchingFixtures.properties(), List.of(new BalancedStrategy()), new MatchingConfigAssembler(), access));
    }

    @Test
    void failsAtStartupIfADefaultWeightsTheCriterionItsStrategyRanksByFirst() {
        Map<MatchingStrategyType, Map<MatchingCriterion, Double>> defaults = strategyDefaults();
        defaults.put(COURSE_FIRST, evenWeights());
        MatchingProperties properties = new MatchingProperties(BALANCED, defaults, MatchingFixtures.properties().quality());

        assertThrows(IllegalStateException.class, () -> new MatchingConfigService(weightRows, strategyRows, properties,
                STRATEGIES, new MatchingConfigAssembler(), access));
    }
}
