package com.studybuddy.music;

import com.studybuddy.common.error.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/** Finds playlists and starts them on the student's in-browser Spotify player. */
@Service
public class SpotifyPlaybackService {
    private static final Logger LOG = LoggerFactory.getLogger(SpotifyPlaybackService.class);

    private final SpotifyConnectionService connections;
    private final SpotifyApiClient api;
    private final SpotifyProperties properties;
    private final SpotifyAssembler assembler;

    public SpotifyPlaybackService(SpotifyConnectionService connections, SpotifyApiClient api,
            SpotifyProperties properties, SpotifyAssembler assembler) {
        this.connections = connections;
        this.api = api;
        this.properties = properties;
        this.assembler = assembler;
    }

    public List<SpotifyPlaylistDto> searchPlaylists(Long studentId, String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidInputException("query", "Enter something to search for");
        }
        if (trimmed.length() > properties.maxQueryLength()) {
            throw new InvalidInputException("query", "Search must be at most " + properties.maxQueryLength() + " characters");
        }
        String accessToken = connections.currentTokens(studentId).accessToken();
        try {
            return api.searchPlaylists(accessToken, trimmed).stream().map(assembler::toDto).toList();
        } catch (IOException failure) {
            LOG.warn("Spotify search failed: {}", failure.getMessage());
            throw new SpotifyUnavailableException("Spotify search is unavailable right now; try again later");
        }
    }

    public void play(Long studentId, SpotifyPlayRequest request) {
        String accessToken = connections.currentTokens(studentId).accessToken();
        try {
            api.playPlaylist(accessToken, request.deviceId(), request.playlistUri());
        } catch (IOException failure) {
            LOG.warn("Spotify playback failed: {}", failure.getMessage());
            throw new SpotifyUnavailableException("Spotify playback is unavailable right now; try again later");
        }
    }
}
