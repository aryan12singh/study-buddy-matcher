package com.studybuddy.studyroom;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Sent regularly by each open room tab to renew its presence lease and report its presence state.
 */
public record RoomHeartbeatInput(@NotNull UUID clientId, @NotNull PresenceState presence) {}
