package com.studybuddy.forest;

import java.time.Instant;

/** One planted tree in a student's forest. */
public record TreeDto(Long id, TreeSpecies species, TreeLight light, long focusedMinutes,
    Instant startedAt, Instant plantedAt, String groupName) {}
