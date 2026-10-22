package com.studybuddy.music;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Uses the existing authenticated-student API guard, except {@code /callback}: Spotify's redirect
 * carries no app token, so that one request is identified by its single-use {@code state} instead.
 */
@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {
    private final SpotifyConnectionService connections;
    private final SpotifyPlaybackService playback;

    public SpotifyController(SpotifyConnectionService connections, SpotifyPlaybackService playback) {
        this.connections = connections;
        this.playback = playback;
    }

    @GetMapping("/status")
    public SpotifyStatusDto status(@AuthenticationPrincipal AccountPrincipal actor) {
        return connections.status(actor.id());
    }

    @PostMapping("/authorization")
    public SpotifyAuthorizationDto startAuthorization(@AuthenticationPrincipal AccountPrincipal actor) {
        return connections.startAuthorization(actor.id());
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state,
            @RequestParam(required = false) String code, @RequestParam(required = false) String error) {
        String next = connections.completeAuthorization(state, code, error);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(next)).build();
    }

    @GetMapping("/token")
    public SpotifyAccessTokenDto accessToken(@AuthenticationPrincipal AccountPrincipal actor) {
        return connections.accessToken(actor.id());
    }

    @DeleteMapping("/connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@AuthenticationPrincipal AccountPrincipal actor) {
        connections.disconnect(actor.id());
    }

    @GetMapping("/playlists")
    public List<SpotifyPlaylistDto> searchPlaylists(@AuthenticationPrincipal AccountPrincipal actor,
            @RequestParam(defaultValue = "") String query) {
        return playback.searchPlaylists(actor.id(), query);
    }

    @PostMapping("/play")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void play(@AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody SpotifyPlayRequest request) {
        playback.play(actor.id(), request);
    }
}
