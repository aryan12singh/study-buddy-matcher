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

    @Test void stateIdentifiesTheStudentOnceOnly() {
        when(clock.instant()).thenReturn(now);
        String state = store.start(7L);
        assertEquals(7L, store.finish(state).orElseThrow());
        assertTrue(store.finish(state).isEmpty());
    }

    @Test void statesAreUnpredictableAndUnique() {
        when(clock.instant()).thenReturn(now);
        assertNotEquals(store.start(7L), store.start(7L));
    }

    @Test void expiredOrUnknownStateIsRejected() {
        when(clock.instant()).thenReturn(now, now.plusSeconds(601));
        String state = store.start(7L);
        assertTrue(store.finish(state).isEmpty());
        assertTrue(store.finish("made-up").isEmpty());
        assertTrue(store.finish(null).isEmpty());
    }
}
