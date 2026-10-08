package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoomHeartbeatInput(@NotNull UUID clientId, @NotNull PresenceState presence) {}
