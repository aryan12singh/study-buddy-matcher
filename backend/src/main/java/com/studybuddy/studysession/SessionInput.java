package com.studybuddy.studysession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Details submitted to create or edit a study session. Times are local to {@code timeZone};
 * {@code expectedVersion} is required when editing so a stale edit is refused.
 */
public record SessionInput(@NotBlank @Size(max = 255) String title,
    @NotNull LocalDateTime startLocal, @NotNull LocalDateTime endLocal,
    @NotBlank @Size(max = 100) String timeZone, @NotNull @Positive Integer occurrenceCount,
    @Size(max = 4000) String agenda, @Size(max = 255) String location,
    @Size(max = 2048) String roomLink, Long expectedVersion) { }
