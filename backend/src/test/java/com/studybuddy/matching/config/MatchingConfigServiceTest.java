package com.studybuddy.matching.config;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.matching.MatchingFixtures;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static com.studybuddy.matching.MatchingFixtures.strategyDefaults;
import static com.studybuddy.matching.strategy.MatchingStrategyType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingConfigServiceTest {
    private static final long ADMIN_ID = 1L;
    private static final Set<MatchingCriterion> ALL = EnumSet.allOf(MatchingCriterion.class);
    private static final List<MatchingStrategy> STRATEGIES = List.of(new BalancedStrategy(), new AvailabilityFirstStrategy(), new CourseFirstStrategy());
    @Mock private MatchingConfigRepository weightRows;
    @Mock private MatchingStrategySettingRepository strategyRows;
    @Mock private AccountAccess access;
    private MatchingConfigService service;

    @BeforeEach void setUp() {
        service = new MatchingConfigService(weightRows, strategyRows, MatchingFixtures.properties(), STRATEGIES,
                new MatchingConfigAssembler(), access);
    }

    private UpdateMatchingConfigRequest request(Map<MatchingStrategyType, Map<MatchingCriterion, Double>> weights) {
        return new UpdateMatchingConfigRequest(BALANCED, weights, ALL, 0.0, 0L);
    }
    private MatchingStrategySetting prepareSave() {
        MatchingStrategySetting row = new MatchingStrategySetting(BALANCED);
        when(strategyRows.lockSingleton()).thenReturn(Optional.of(row));
        return row;
    }

    @Test void readsAllFiveDefaultsAtVersionZeroWithoutWriting() {
        var config = service.get(ADMIN_ID);
        assertEquals(strategyDefaults(), config.weights());
        assertEquals(ALL, config.enabledCriteria());
        assertTrue(config.usingDefaults());
        assertEquals(0, config.version());
        assertEquals(0, config.minScore());
        verify(access).requireAdmin(ADMIN_ID);
        verify(strategyRows, never()).initialize(any());
    }

    @Test void savesAllStrategyWeightsAndIncrementsVersion() {
        MatchingStrategySetting row = prepareSave();
        var saved = service.update(request(strategyDefaults()), ADMIN_ID);
        assertEquals(1, saved.version());
        assertFalse(saved.usingDefaults());
        verify(weightRows, times(15)).save(any());
        verify(strategyRows).initialize("BALANCED");
        verify(strategyRows).save(row);
    }

    @Test void preservesFractionalPrecisionOnSaveReadAndSearchSnapshot() {
        MatchingStrategySetting row = prepareSave();
        var weights = strategyDefaults();
        weights.get(BALANCED).replaceAll((criterion, weight) -> 0.0);
        weights.get(BALANCED).put(MatchingCriterion.COURSE, 0.004);
        weights.get(BALANCED).put(MatchingCriterion.AVAILABILITY, 0.996);
        List<MatchingConfig> rows = new ArrayList<>();
        when(weightRows.save(any())).thenAnswer(call -> { rows.add(call.getArgument(0)); return call.getArgument(0); });
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.of(row));
        when(weightRows.findAll()).thenReturn(rows);
        var saved = service.update(request(weights), ADMIN_ID);
        assertEquals(saved, service.get(ADMIN_ID));
        assertEquals(weights.get(BALANCED), service.snapshot().weights().values());
        assertEquals(1, service.snapshot().version());
    }

    @Test void validFractionalDefaultsAndSumToleranceRemainUnrounded() {
        var weights = strategyDefaults();
        weights.get(BALANCED).put(MatchingCriterion.COURSE, 0.2001);
        var defaults = MatchingFixtures.properties();
        service = new MatchingConfigService(weightRows, strategyRows,
                new MatchingProperties(BALANCED, weights, defaults.quality(), defaults.scoring(), ALL, 0, 20, 50), STRATEGIES,
                new MatchingConfigAssembler(), access);
        assertEquals(0.2001, service.snapshot().weights().weightOf(MatchingCriterion.COURSE));
    }

    @Test void normalizesLegacyOverridesOnlyWhenTheirTotalsAreInvalid() {
        MatchingStrategySetting row = new MatchingStrategySetting(BALANCED);
        row.saveOverride(BALANCED, ALL, 0.0);
        when(strategyRows.findFirstByOrderByIdAsc()).thenReturn(Optional.of(row));
        when(weightRows.findAll()).thenReturn(List.of(new MatchingConfig(BALANCED, MatchingCriterion.COURSE, 0.9)));
        assertEquals(0.53, service.snapshot().weights().weightOf(MatchingCriterion.COURSE));
        assertEquals(0.11, service.snapshot().weights().weightOf(MatchingCriterion.GROUP_SIZE));
    }

    @Test void priorityCriterionCanBeTheOnlyEnabledWeightedCriterion() {
        prepareSave();
        var weights = strategyDefaults();
        weights.values().forEach(values -> values.replaceAll((criterion, weight) -> criterion == MatchingCriterion.COURSE ? 1.0 : 0.0));
        var saved = service.update(new UpdateMatchingConfigRequest(COURSE_FIRST, weights, Set.of(MatchingCriterion.COURSE), 0.8, 0L), ADMIN_ID);
        assertEquals(1.0, saved.weights().get(COURSE_FIRST).get(MatchingCriterion.COURSE));
        assertEquals(0.8, saved.minScore());
    }

    @Test void disabledCriteriaCannotKeepWeightAndAtLeastOneMustRemainEnabled() {
        assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, strategyDefaults(), Set.of(), 0.0, 0L), ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class,
                () -> service.update(new UpdateMatchingConfigRequest(BALANCED, strategyDefaults(), Set.of(MatchingCriterion.COURSE), 0.0, 0L), ADMIN_ID));
        verify(weightRows, never()).save(any());
    }

    @Test void negativeZeroTotalMissingAndInvalidSumWeightsAreRejectedBeforeWriting() {
        for (Double value : new Double[]{-0.1, Double.NaN, Double.POSITIVE_INFINITY, 10.0, null}) {
            var weights = strategyDefaults();
            weights.get(BALANCED).put(MatchingCriterion.COURSE, value);
            assertThrows(InvalidMatchingConfigException.class, () -> service.update(request(weights), ADMIN_ID));
        }
        var zero = strategyDefaults();
        zero.get(BALANCED).replaceAll((criterion, weight) -> 0.0);
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(request(zero), ADMIN_ID));
        var missing = strategyDefaults(); missing.remove(COURSE_FIRST);
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(request(missing), ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(request(null), ADMIN_ID));
        verify(weightRows, never()).save(any());
        verify(strategyRows, never()).initialize(any());
    }

    @Test void thresholdMustBeFiniteAndBetweenZeroAndOne() {
        for (Double threshold : new Double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY, null}) {
            assertThrows(InvalidMatchingConfigException.class,
                    () -> service.update(new UpdateMatchingConfigRequest(BALANCED, strategyDefaults(), ALL, threshold, 0L), ADMIN_ID));
        }
        verify(weightRows, never()).save(any());
    }

    @Test void staleVersionDoesNotWriteAnyWeights() {
        MatchingStrategySetting row = prepareSave(); row.saveOverride(BALANCED, ALL, 0.0);
        assertThrows(StaleMatchingConfigException.class, () -> service.update(request(strategyDefaults()), ADMIN_ID));
        verify(weightRows, never()).save(any());
        verify(strategyRows, never()).save(any());
    }

    @Test void restoresStartupDefaultsAtANewerVersionAndDeletesOverrides() {
        MatchingStrategySetting row = prepareSave(); row.saveOverride(COURSE_FIRST, Set.of(MatchingCriterion.COURSE), 0.9);
        var restored = service.restore(new RestoreMatchingDefaultsRequest(1L), ADMIN_ID);
        assertTrue(restored.usingDefaults());
        assertEquals(2, restored.version());
        assertEquals(strategyDefaults(), restored.weights());
        assertEquals(ALL, restored.enabledCriteria());
        assertEquals(0.0, restored.minScore());
        verify(weightRows).deleteAll();
        verify(strategyRows).save(row);
    }

    @Test void missingStrategyVersionOrEnabledListIsRejected() {
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(null, ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(new UpdateMatchingConfigRequest(null, strategyDefaults(), ALL, 0.0, 0L), ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(new UpdateMatchingConfigRequest(BALANCED, strategyDefaults(), null, 0.0, 0L), ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class, () -> service.update(new UpdateMatchingConfigRequest(BALANCED, strategyDefaults(), ALL, 0.0, null), ADMIN_ID));
        assertThrows(InvalidMatchingConfigException.class, () -> service.restore(null, ADMIN_ID));
    }

    @Test void nonAdminCannotReadSaveOrRestore() {
        when(access.requireAdmin(ADMIN_ID)).thenThrow(new ForbiddenActionException("An administrator account is required"));
        doThrow(new ForbiddenActionException("An administrator account is required")).when(access).lockAdmin(ADMIN_ID);
        assertThrows(ForbiddenActionException.class, () -> service.get(ADMIN_ID));
        assertThrows(ForbiddenActionException.class, () -> service.update(request(strategyDefaults()), ADMIN_ID));
        assertThrows(ForbiddenActionException.class, () -> service.restore(new RestoreMatchingDefaultsRequest(0L), ADMIN_ID));
        verify(weightRows, never()).save(any());
    }

    @Test void missingStrategyImplementationFailsAtStartup() {
        assertThrows(IllegalStateException.class, () -> new MatchingConfigService(weightRows, strategyRows,
                MatchingFixtures.properties(), List.of(new BalancedStrategy()), new MatchingConfigAssembler(), access));
    }
}
