package com.studybuddy.matching.config;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.InternalInvariantException;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.strategy.MatchingStrategy;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persisted overrides, startup defaults and one versioned settings snapshot for each search. */
@Service
@Transactional
public class MatchingConfigService {
    private final MatchingConfigRepository weightRows;
    private final MatchingStrategySettingRepository strategyRows;
    private final MatchingProperties defaults;
    private final Map<MatchingStrategyType, MatchingStrategy> strategies;
    private final MatchingConfigAssembler assembler;
    private final AccountAccess access;

    public MatchingConfigService(MatchingConfigRepository weightRows, MatchingStrategySettingRepository strategyRows,
            MatchingProperties defaults, List<MatchingStrategy> strategies, MatchingConfigAssembler assembler, AccountAccess access) {
        this.weightRows = weightRows;
        this.strategyRows = strategyRows;
        this.defaults = defaults;
        this.strategies = byType(strategies);
        this.assembler = assembler;
        this.access = access;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MatchingConfigDto get(Long actorId) {
        access.requireAdmin(actorId);
        return effectiveConfiguration();
    }

    public MatchingConfigDto update(UpdateMatchingConfigRequest request, Long actorId) {
        access.lockAdmin(actorId);
        if (request == null || request.activeStrategy() == null) {
            throw new InvalidMatchingConfigException("An active strategy is required");
        }
        Set<MatchingCriterion> enabled = validEnabled(request.enabledCriteria());
        double minScore = validThreshold(request.minScore());
        Map<MatchingStrategyType, MatchingWeights> weights = validWeights(request.weights(), enabled);
        MatchingStrategySetting setting = lockSettings(request.expectedVersion());
        weights.forEach(this::saveWeights);
        setting.saveOverride(request.activeStrategy(), enabled, minScore);
        strategyRows.save(setting);
        return assembler.toDto(request.activeStrategy(), weights, enabled, minScore, setting.getConfigVersion(), false);
    }

    public MatchingConfigDto restore(RestoreMatchingDefaultsRequest request, Long actorId) {
        access.lockAdmin(actorId);
        MatchingStrategySetting setting = lockSettings(request == null ? null : request.expectedVersion());
        weightRows.deleteAll();
        setting.restoreDefaults();
        strategyRows.save(setting);
        return defaultsDto(setting.getConfigVersion());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MatchingConfigSnapshot snapshot() {
        MatchingConfigDto settings = effectiveConfiguration();
        return new MatchingConfigSnapshot(strategies.get(settings.activeStrategy()),
                new MatchingWeights(settings.weights().get(settings.activeStrategy())), settings.enabledCriteria(),
                settings.minScore(), settings.version());
    }

    private MatchingConfigDto effectiveConfiguration() {
        MatchingStrategySetting setting = strategyRows.findFirstByOrderByIdAsc().orElse(null);
        if (setting == null || !setting.hasOverride()) {
            return defaultsDto(setting == null ? 0 : setting.getConfigVersion());
        }
        return assembler.toDto(setting.getActiveStrategy(), allWeights(), setting.getEnabledCriteria(),
                setting.getMinScore(), setting.getConfigVersion(), false);
    }

    private MatchingConfigDto defaultsDto(long version) {
        Map<MatchingStrategyType, MatchingWeights> weights = new EnumMap<>(MatchingStrategyType.class);
        for (MatchingStrategyType type : MatchingStrategyType.values()) { weights.put(type, defaults.weightsFor(type)); }
        return assembler.toDto(defaults.defaultStrategy(), weights, defaults.defaultEnabledCriteria(),
                defaults.defaultMinScore(), version, true);
    }

    private MatchingStrategySetting lockSettings(Long expectedVersion) {
        if (expectedVersion == null || expectedVersion < 0) {
            throw new InvalidMatchingConfigException("A non-negative expected version is required");
        }
        strategyRows.initialize(defaults.defaultStrategy().name());
        MatchingStrategySetting setting = strategyRows.lockSingleton().orElseThrow();
        if (setting.getConfigVersion() != expectedVersion) { throw new StaleMatchingConfigException(); }
        return setting;
    }

    private Map<MatchingStrategyType, MatchingWeights> allWeights() {
        List<MatchingConfig> rows = weightRows.findAll();
        Map<MatchingStrategyType, MatchingWeights> weights = new EnumMap<>(MatchingStrategyType.class);
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            Map<MatchingCriterion, Double> values = new EnumMap<>(defaults.weightsFor(type).values());
            rows.stream().filter(row -> row.getStrategy() == type)
                    .forEach(row -> values.put(row.getCriterion(), row.getWeight()));
            double sum = values.values().stream().mapToDouble(Double::doubleValue).sum();
            weights.put(type, Math.abs(sum - 1.0) <= MatchingWeights.SUM_TOLERANCE
                    ? new MatchingWeights(values) : MatchingWeights.rescaledToOne(values));
        }
        return weights;
    }

    private static Set<MatchingCriterion> validEnabled(Set<MatchingCriterion> enabled) {
        if (enabled == null || enabled.isEmpty() || enabled.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidMatchingConfigException("At least one criterion must be enabled");
        }
        return Set.copyOf(enabled);
    }

    private static double validThreshold(Double threshold) {
        if (threshold == null || !Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
            throw new InvalidMatchingConfigException("Minimum score must be between 0 and 1");
        }
        return threshold;
    }

    private Map<MatchingStrategyType, MatchingWeights> validWeights(
            Map<MatchingStrategyType, Map<MatchingCriterion, Double>> requested, Set<MatchingCriterion> enabled) {
        if (requested == null) { throw new InvalidMatchingConfigException("Weights for every strategy are required"); }
        Map<MatchingStrategyType, MatchingWeights> weights = new EnumMap<>(MatchingStrategyType.class);
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            Map<MatchingCriterion, Double> values = requested.get(type);
            if (values == null) { throw new InvalidMatchingConfigException("Weights for " + type.label() + " are required"); }
            for (MatchingCriterion criterion : MatchingCriterion.values()) {
                if (!enabled.contains(criterion) && values.get(criterion) != null && values.get(criterion) != 0.0) {
                    throw new InvalidMatchingConfigException("Disabled criterion " + criterion + " must have zero weight");
                }
            }
            try { weights.put(type, new MatchingWeights(values)); }
            catch (IllegalArgumentException invalid) {
                throw new InvalidMatchingConfigException(type.label() + ": " + invalid.getMessage());
            }
        }
        return weights;
    }

    private void saveWeights(MatchingStrategyType type, MatchingWeights weights) {
        for (MatchingCriterion criterion : MatchingCriterion.values()) {
            double weight = weights.weightOf(criterion);
            MatchingConfig row = weightRows.findByStrategyAndCriterion(type, criterion)
                    .orElseGet(() -> new MatchingConfig(type, criterion, weight));
            row.setWeight(weight);
            weightRows.save(row);
        }
    }

    private static Map<MatchingStrategyType, MatchingStrategy> byType(List<MatchingStrategy> implementations) {
        Map<MatchingStrategyType, MatchingStrategy> indexed = new EnumMap<>(MatchingStrategyType.class);
        implementations.forEach(strategy -> indexed.put(strategy.type(), strategy));
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            if (!indexed.containsKey(type)) { throw new InternalInvariantException("No MatchingStrategy implementation for " + type); }
        }
        return indexed;
    }
}
