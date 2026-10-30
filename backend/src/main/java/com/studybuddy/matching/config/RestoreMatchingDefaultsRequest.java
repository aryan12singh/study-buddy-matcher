package com.studybuddy.matching.config;

import jakarta.validation.constraints.NotNull;

/**
 * Asks to restore the configured default matching settings. {@code expectedVersion} is the version
 * the administrator last loaded, so a restore over someone else's newer save is refused.
 */
public record RestoreMatchingDefaultsRequest(@NotNull Long expectedVersion) {
}
