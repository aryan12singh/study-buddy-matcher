package com.studybuddy.music;

import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpotifyPlaybackServiceTest {
    private final SpotifyConnectionService connections = mock(SpotifyConnectionService.class);
    private final SpotifyApiClient api = mock(SpotifyApiClient.class);
    private final SpotifyPlaybackService service = new SpotifyPlaybackService(connections, api,
            SpotifyTestSettings.configured(), new SpotifyAssembler());

    private void connected() {
        when(connections.currentTokens(7L)).thenReturn(new SpotifyTokens("access", "refresh", Instant.MAX));
    }

    @Test void searchUsesTheStudentsTokenAndTrimmedQuery() throws Exception {
        connected();
        when(api.searchPlaylists("access", "lofi")).thenReturn(List.of(new SpotifyPlaylist("p1", "spotify:playlist:p1", "Lofi", "Spotify", null)));
        assertEquals(List.of(new SpotifyPlaylistDto("p1", "spotify:playlist:p1", "Lofi", "Spotify", null)),
                service.searchPlaylists(7L, " lofi "));
    }

    @Test void blankOrOverlongSearchIsRejectedBeforeSpotify() {
        assertThrows(InvalidInputException.class, () -> service.searchPlaylists(7L, " "));
        assertThrows(InvalidInputException.class, () -> service.searchPlaylists(7L, "a".repeat(101)));
        verifyNoInteractions(connections, api);
    }

    @Test void providerFailureReportsUnavailable() throws Exception {
        connected();
        when(api.searchPlaylists("access", "lofi")).thenThrow(new IOException("down"));
        doThrow(new IOException("down")).when(api).playPlaylist("access", "device", "spotify:playlist:p1");
        assertThrows(SpotifyUnavailableException.class, () -> service.searchPlaylists(7L, "lofi"));
        assertThrows(SpotifyUnavailableException.class, () -> service.play(7L, new SpotifyPlayRequest("device", "spotify:playlist:p1")));
    }

    @Test void playStartsThePlaylistOnTheGivenDevice() throws Exception {
        connected();
        service.play(7L, new SpotifyPlayRequest("device", "spotify:playlist:p1"));
        verify(api).playPlaylist("access", "device", "spotify:playlist:p1");
    }
}
