package com.studybuddy.matchrequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SendMatchRequest(@NotNull @Positive Long receiverId, @Size(max = 255) String message, @Valid MatchRequestContext context) {
}
