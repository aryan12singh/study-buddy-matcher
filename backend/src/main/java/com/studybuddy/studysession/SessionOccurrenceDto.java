package com.studybuddy.studysession;

import java.time.Instant;

public record SessionOccurrenceDto(Instant startAt, Instant endAt) { }
