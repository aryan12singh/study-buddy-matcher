package com.studybuddy.matching.config;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.strategy.MatchingStrategy;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and saves the admin-configured matching settings: the active strategy
 * and each strategy's own weights. Anything an admin has not saved yet falls
 * back to the defaults in {@code app.matching.*}.
 */
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
            MatchingProperties defaults, List<MatchingStrategy> strategies, MatchingConfigAssembler assembler,
            AccountAccess access) {
        this.weightRows = weightRows;
        this.strategyRows = strategyRows;
        this.defaults = defaults;
        this.strategies = byType(strategies);
        this.assembler = assembler;
        this.access = access;
        requireDefaultsSkipRankedFirstCriteria();
    }

    @Transactional(readOnly = true)
    public MatchingConfigDto get(Long actorId) {
        access.requireAdmin(actorId);
        return assembler.toDto(activeStrategyType(), allWeights(), strategies);
    }

    public MatchingConfigDto update(UpdateMatchingConfigRequest request, Long actorId) {
        access.lockAdmin(actorId);
        if (request == null || request.activeStrategy() == null) {
            throw new InvalidMatchingConfigException("An active strategy is required");
        }
        Map<MatchingStrategyType, MatchingWeights> weights = validWeights(request.weights());
        weights.forEach(this::saveWeights);
        saveActiveStrategy(request.activeStrategy());
        return assembler.toDto(request.activeStrategy(), weights, strategies);
    }

    /** The weights the matching engine scores with: the active strategy's. */
    @Transactional(readOnly = true)
    public MatchingWeights currentWeights() {
        return savedWeights(activeStrategyType(), weightRows.findAll());
    }

    /** The strategy the matching engine ranks with. */
    @Transactional(readOnly = true)
    public MatchingStrategy activeStrategy() {
        return strategies.get(activeStrategyType());
    }

    private MatchingStrategyType activeStrategyType() {
        return strategyRows.findFirstByOrderByIdAsc()
                .map(MatchingStrategySetting::getActiveStrategy)
                .orElse(defaults.defaultStrategy());
    }

    private Map<MatchingStrategyType, MatchingWeights> allWeights() {
        List<MatchingConfig> rows = weightRows.findAll();
        Map<MatchingStrategyType, MatchingWeights> weights = new EnumMap<>(MatchingStrategyType.class);
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            weights.put(type, savedWeights(type, rows));
        }
        return weights;
    }

    /**
     * A strategy's saved weights over its defaults. The criterion it ranks by first is
     * always 0, and weights saved before they had to add up to 1 are rescaled.
     */
    private MatchingWeights savedWeights(MatchingStrategyType type, List<MatchingConfig> rows) {
        Map<MatchingCriterion, Double> values = new EnumMap<>(defaults.weightsFor(type).values());
        rows.stream()
                .filter(row -> row.getStrategy() == type)
                .forEach(row -> values.put(row.getCriterion(), row.getWeight()));
        unweightedCriteria(type).forEach(criterion -> values.put(criterion, 0.0));
        return MatchingWeights.rescaledToOne(values);
    }

    private Map<MatchingStrategyType, MatchingWeights> validWeights(
            Map<MatchingStrategyType, Map<MatchingCriterion, Double>> requested) {
        if (requested == null) {
            throw new InvalidMatchingConfigException("Weights for every strategy are required");
        }
        Map<MatchingStrategyType, MatchingWeights> weights = new EnumMap<>(MatchingStrategyType.class);
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            weights.put(type, validWeights(type, requested.get(type)));
        }
        return weights;
    }

    private MatchingWeights validWeights(MatchingStrategyType type, Map<MatchingCriterion, Double> requested) {
        if (requested == null) {
            throw new InvalidMatchingConfigException("Weights for " + type.label() + " are required");
        }
        Map<MatchingCriterion, Double> values = new EnumMap<>(MatchingCriterion.class);
        for (MatchingCriterion criterion : MatchingCriterion.values()) {
            values.put(criterion, requested.get(criterion));
        }
        for (MatchingCriterion criterion : unweightedCriteria(type)) {
            Double weight = values.get(criterion);
            if (weight != null && weight != 0.0) {
                throw new InvalidMatchingConfigException(type.label() + " already ranks by " + criterion
                        + ", so it cannot also weight it");
            }
            values.put(criterion, 0.0);
        }
        try {
            return new MatchingWeights(values);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidMatchingConfigException(type.label() + ": " + invalid.getMessage());
        }
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

    private void saveActiveStrategy(MatchingStrategyType type) {
        MatchingStrategySetting row = strategyRows.findFirstByOrderByIdAsc()
                .orElseGet(() -> new MatchingStrategySetting(type));
        row.setActiveStrategy(type);
        strategyRows.save(row);
    }

    private List<MatchingCriterion> unweightedCriteria(MatchingStrategyType type) {
        var weighted = strategies.get(type).weightedCriteria();
        return List.of(MatchingCriterion.values()).stream().filter(criterion -> !weighted.contains(criterion)).toList();
    }

    /** A default weight on a criterion the strategy ranks by first would be silently ignored, so refuse to start. */
    private void requireDefaultsSkipRankedFirstCriteria() {
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            MatchingWeights defaultWeights = defaults.weightsFor(type);
            for (MatchingCriterion criterion : unweightedCriteria(type)) {
                if (defaultWeights.weightOf(criterion) != 0.0) {
                    throw new IllegalStateException("The default weights for " + type + " must leave out " + criterion);
                }
            }
        }
    }

    /** Indexes the strategies by type, failing at startup if any type has no implementation. */
    private static Map<MatchingStrategyType, MatchingStrategy> byType(List<MatchingStrategy> strategies) {
        Map<MatchingStrategyType, MatchingStrategy> byType = new EnumMap<>(MatchingStrategyType.class);
        strategies.forEach(strategy -> byType.put(strategy.type(), strategy));
        for (MatchingStrategyType type : MatchingStrategyType.values()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException("No MatchingStrategy implementation for " + type);
            }
        }
        return byType;
    }
}
