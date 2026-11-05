package com.studybuddy.music;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpotifyAuthorizationStoreTest {
    private final Instant now = Instant.parse("2026-10-22T08:00:00Z");
    private final Clock clock = mock(Clock.class);
    private final SpotifyAuthorizationStore store = new SpotifyAuthorizationStore(SpotifyTestSettings.configured(), clock);

    @Test void onlyTheStudentWhoStartedCanFinishAndOnlyOnce() {
        when(clock.instant()).thenReturn(now);
        String state = store.start(7L);
        assertTrue(store.isPending(state));
        assertTrue(store.finish(state, 7L));
        assertFalse(store.finish(state, 7L));
        assertFalse(store.isPending(state));
    }

    @Test void someoneElseFinishingUsesTheSignInUpWithoutConnectingIt() {
        when(clock.instant()).thenReturn(now);
        String state = store.start(7L);
        assertFalse(store.finish(state, 8L));
        assertFalse(store.finish(state, 7L), "A sign-in someone else tried to finish cannot be reused");
    }

    @Test void statesAreUnpredictableAndUnique() {
        when(clock.instant()).thenReturn(now);
        assertNotEquals(store.start(7L), store.start(7L));
    }

    @Test void expiredOrUnknownStateIsRejected() {
        when(clock.instant()).thenReturn(now, now.plusSeconds(601));
        String state = store.start(7L);
        assertFalse(store.finish(state, 7L));
        assertFalse(store.finish("made-up", 7L));
        assertFalse(store.finish(null, 7L));
        assertFalse(store.isPending(null));
    }
}
