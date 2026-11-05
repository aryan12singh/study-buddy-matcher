package com.studybuddy.music;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Uses the existing authenticated-student API guard, except {@code /callback}: Spotify's redirect
 * carries no app token, so it only relays the sign-in to the browser, which finishes it signed in.
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
        String next = connections.relayCallback(state, code, error);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(next)).build();
    }

    @PostMapping("/authorization/complete")
    public SpotifyConnectionOutcomeDto finishAuthorization(@AuthenticationPrincipal AccountPrincipal actor,
            @Valid @RequestBody SpotifyAuthorizationInput input) {
        return connections.finishAuthorization(actor.id(), input);
    }

    @GetMapping("/token")
    public SpotifyAccessTokenDto accessToken(@AuthenticationPrincipal AccountPrincipal actor) {
        return connections.accessToken(actor.id());
    }
}
