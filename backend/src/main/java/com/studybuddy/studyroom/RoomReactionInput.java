package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;

/** A reaction a member sends to everyone in the group room. */
public record RoomReactionInput(@NotNull RoomReactionType type) {}
