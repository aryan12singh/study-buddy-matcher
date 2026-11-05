package com.studybuddy.studysession;

import java.time.Instant;

/** One weekly occurrence of a study session, as instants. */
public record SessionOccurrenceDto(Instant startAt, Instant endAt) { }
