package com.studybuddy.music;

import jakarta.validation.constraints.NotBlank;

/** One ambient channel the student can play, and the provider search that fills it. */
public record AmbientCategory(@NotBlank String id, @NotBlank String label, @NotBlank String query) {
}
