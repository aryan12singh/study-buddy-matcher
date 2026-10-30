package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Joins a group room from one browser tab, identified by a random client id. */
public record RoomJoinInput(@NotNull UUID clientId) {}
