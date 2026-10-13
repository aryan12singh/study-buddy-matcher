package com.studybuddy.matching.config;

import jakarta.validation.constraints.NotNull;

public record RestoreMatchingDefaultsRequest(@NotNull Long expectedVersion) {
}
