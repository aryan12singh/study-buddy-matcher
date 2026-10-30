package com.studybuddy.forest;

import java.time.Instant;

/**
 * One planted tree in a student's forest. {@code variant} is which kind of tree it is within its
 * species, and {@code plot} where it stands; both were drawn at random by the server.
 */
public record TreeDto(Long id, TreeSpecies species, int variant, TreeLight light, long focusedMinutes,
    Instant startedAt, Instant plantedAt, String groupName, int plot) {}
