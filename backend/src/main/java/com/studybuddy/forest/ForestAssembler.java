package com.studybuddy.forest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ForestAssembler {
    private final TreeGrowth growth;
    private final ForestProperties properties;

    public ForestAssembler(TreeGrowth growth, ForestProperties properties) {
        this.growth = growth;
        this.properties = properties;
    }

    public ForestDto toForest(List<FocusVisit> planted) {
        List<TreeDto> trees = planted.stream()
            .flatMap(visit -> growth.speciesFor(minutes(visit.focused())).stream()
                .map(species -> new TreeDto(visit.getId(), species, visit.getVariant(), lightOf(visit),
                    minutes(visit.focused()), visit.getStartedAt(), visit.plantedAt(), visit.getGroupName(), visit.getPlot())))
            .toList();
        long total = trees.stream().mapToLong(TreeDto::focusedMinutes).sum();
        long longest = trees.stream().mapToLong(TreeDto::focusedMinutes).max().orElse(0);
        List<TreeStageDto> stages = Arrays.stream(TreeSpecies.values())
            .map(species -> new TreeStageDto(species, growth.minutesFor(species))).toList();
        return new ForestDto(trees, trees.size(), total, longest, stages, properties.variantsPerSpecies());
    }

    public GrowingTreeDto toGrowing(FocusVisit visit) {
        long minutes = minutes(visit.focused());
        var next = growth.nextAfter(minutes);
        return new GrowingTreeDto(true, minutes, growth.speciesFor(minutes).orElse(null), visit.getVariant(), next.orElse(null),
            next.map(species -> (int) (growth.minutesFor(species) - minutes)).orElse(null), lightOf(visit));
    }

    public GrowingTreeDto notVisiting() {
        var first = TreeSpecies.values()[0];
        return new GrowingTreeDto(false, 0, null, 0, first, growth.minutesFor(first), null);
    }

    private TreeLight lightOf(FocusVisit visit) {
        return TreeLight.at(visit.getStartedAt().atZone(properties.zone()).toLocalTime(), properties.lightStarts());
    }

    private static long minutes(Duration focused) {
        return focused.toMinutes();
    }
}
