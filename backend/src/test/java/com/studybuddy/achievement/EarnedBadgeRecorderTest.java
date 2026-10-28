package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EarnedBadgeRecorderTest {
    private static final Long STUDENT = 7L;
    private final EarnedBadgeRepository repository = mock(EarnedBadgeRepository.class);
    private final EarnedBadgeRecorder recorder = new EarnedBadgeRecorder(repository);

    private static EarnedBadge saved(BadgeDefinition badge, BadgeTier tier) {
        EarnedBadge row = mock(EarnedBadge.class);
        when(row.getBadge()).thenReturn(badge);
        when(row.getBestTier()).thenReturn(tier);
        return row;
    }

    private static Map<BadgeDefinition, BadgeTier> live(Object... pairs) {
        Map<BadgeDefinition, BadgeTier> tiers = new EnumMap<>(BadgeDefinition.class);
        for (int i = 0; i < pairs.length; i += 2) tiers.put((BadgeDefinition) pairs[i], (BadgeTier) pairs[i + 1]);
        return tiers;
    }

    @Test
    void savesANewlyEarnedTier() {
        when(repository.findByStudentId(STUDENT)).thenReturn(List.of());

        Map<BadgeDefinition, BadgeTier> best = recorder.keepHighest(STUDENT, live(BadgeDefinition.FIRST_MOVE, BadgeTier.BRONZE));

        verify(repository).raise(STUDENT, "FIRST_MOVE", "BRONZE");
        assertThat(best).containsEntry(BadgeDefinition.FIRST_MOVE, BadgeTier.BRONZE);
    }

    @Test
    void keepsTheSavedTierWhenTheLiveOneIsLowerOrMissing() {
        List<EarnedBadge> rows = List.of(
                saved(BadgeDefinition.CONNECTOR, BadgeTier.SILVER), saved(BadgeDefinition.TEAM_PLAYER, BadgeTier.BRONZE));
        when(repository.findByStudentId(STUDENT)).thenReturn(rows);

        Map<BadgeDefinition, BadgeTier> best = recorder.keepHighest(STUDENT,
                live(BadgeDefinition.CONNECTOR, BadgeTier.BRONZE, BadgeDefinition.TEAM_PLAYER, null));

        verify(repository, never()).raise(anyLong(), anyString(), anyString());
        assertThat(best).containsEntry(BadgeDefinition.CONNECTOR, BadgeTier.SILVER)
                .containsEntry(BadgeDefinition.TEAM_PLAYER, BadgeTier.BRONZE);
    }

    @Test
    void raisesTheSavedTierWhenTheLiveOneIsHigher() {
        List<EarnedBadge> rows = List.of(saved(BadgeDefinition.CONNECTOR, BadgeTier.BRONZE));
        when(repository.findByStudentId(STUDENT)).thenReturn(rows);

        Map<BadgeDefinition, BadgeTier> best = recorder.keepHighest(STUDENT, live(BadgeDefinition.CONNECTOR, BadgeTier.GOLD));

        verify(repository).raise(STUDENT, "CONNECTOR", "GOLD");
        assertThat(best).containsEntry(BadgeDefinition.CONNECTOR, BadgeTier.GOLD);
    }

    @Test
    void neverEarnedBadgesAreAbsent() {
        when(repository.findByStudentId(STUDENT)).thenReturn(List.of());

        Map<BadgeDefinition, BadgeTier> best = recorder.keepHighest(STUDENT, live(BadgeDefinition.FOUNDER, null));

        verify(repository, never()).raise(anyLong(), anyString(), anyString());
        assertThat(best).isEmpty();
    }
}
