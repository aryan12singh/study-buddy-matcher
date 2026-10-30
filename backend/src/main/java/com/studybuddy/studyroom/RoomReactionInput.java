package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;

public record RoomReactionInput(@NotNull RoomReactionType type) {}
