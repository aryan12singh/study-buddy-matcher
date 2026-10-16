package com.studybuddy.studysession;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CancelSessionInput(@NotNull @PositiveOrZero Long expectedVersion) { }
