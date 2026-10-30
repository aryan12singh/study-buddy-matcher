package com.studybuddy.music;

import com.studybuddy.security.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Uses the existing authenticated-student API guard, except {@code /callback}: Spotify's redirect
 * carries no app token, so that one request is identified by its single-use {@code state} instead.
 */
@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {
    private final SpotifyConnectionService connections;

    public SpotifyController(SpotifyConnectionService connections) {
        this.connections = connections;
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
}
