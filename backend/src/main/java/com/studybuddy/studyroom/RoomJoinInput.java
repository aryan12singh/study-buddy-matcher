package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoomJoinInput(@NotNull UUID clientId) {}
