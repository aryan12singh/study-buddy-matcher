package com.studybuddy.studysession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record SessionInput(@NotBlank @Size(max = 255) String title,
    @NotNull LocalDateTime startLocal, @NotNull LocalDateTime endLocal,
    @NotBlank @Size(max = 100) String timeZone, @NotNull @Positive Integer occurrenceCount,
    @Size(max = 4000) String agenda, @Size(max = 255) String location,
    @Size(max = 2048) String roomLink, Long expectedVersion) { }
