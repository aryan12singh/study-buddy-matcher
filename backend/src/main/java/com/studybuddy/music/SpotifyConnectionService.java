package com.studybuddy.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Clock;

/** Connects a student's Spotify account, keeps its token fresh, and disconnects it. */
@Service
public class SpotifyConnectionService {
    private static final Logger LOG = LoggerFactory.getLogger(SpotifyConnectionService.class);

    /** The outcome the Study Room shows after Spotify sends the student back. */
    enum Outcome { CONNECTED, DENIED, EXPIRED, FAILED }

    private final SpotifyProperties properties;
    private final SpotifyAuthorizationStore authorizations;
    private final SpotifyTokenStore tokens;
    private final SpotifyAccountsClient accounts;
    private final SpotifyAssembler assembler;
    private final Clock clock;

    public SpotifyConnectionService(SpotifyProperties properties, SpotifyAuthorizationStore authorizations,
            SpotifyTokenStore tokens, SpotifyAccountsClient accounts, SpotifyAssembler assembler, Clock clock) {
        this.properties = properties;
        this.authorizations = authorizations;
        this.tokens = tokens;
        this.accounts = accounts;
        this.assembler = assembler;
        this.clock = clock;
    }

    public SpotifyStatusDto status(Long studentId) {
        boolean configured = properties.ready();
        return new SpotifyStatusDto(configured, configured && tokens.find(studentId).isPresent(),
                properties.embedPlaylistId());
    }

    public SpotifyAuthorizationDto startAuthorization(Long studentId) {
        requireConfigured();
        String url = UriComponentsBuilder.fromUriString(properties.accountsUrl()).path("/authorize")
                .queryParam("client_id", properties.clientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", String.join(" ", properties.scopes()))
                .queryParam("state", authorizations.start(studentId))
                .encode().build().toUriString();
        return new SpotifyAuthorizationDto(url);
    }

    /**
     * Handles Spotify's redirect after sign-in and returns where to send the browser next. Never
     * throws. It saves nothing: the code goes on to the browser that completed the sign-in, which
     * finishes it with {@link #finishAuthorization} as the signed-in student.
     */
    public String relayCallback(String state, String code, String error) {
        if (!properties.ready() || !authorizations.isPending(state)) {
            return returnUrl(Outcome.EXPIRED);
        }
        if (error != null || code == null || code.isBlank()) {
            return returnUrl(Outcome.DENIED);
        }
        return UriComponentsBuilder.fromUriString(properties.returnUrl()).queryParam("spotify", "confirm")
                .queryParam("state", state).queryParam("code", code).encode().build().toUriString();
    }

    /**
     * Connects Spotify for the signed-in student, but only if they started this sign-in. Someone
     * who opens another student's sign-in link gets {@code expired}, and their account stays theirs.
     */
    public SpotifyConnectionOutcomeDto finishAuthorization(Long studentId, SpotifyAuthorizationInput input) {
        requireConfigured();
        if (!authorizations.finish(input.state(), studentId)) {
            return outcome(Outcome.EXPIRED);
        }
        try {
            tokens.save(studentId, accounts.exchangeCode(input.code()));
            return outcome(Outcome.CONNECTED);
        } catch (IOException failure) {
            LOG.warn("Spotify sign-in could not be completed: {}", failure.getMessage());
            return outcome(Outcome.FAILED);
        }
    }

    public SpotifyAccessTokenDto accessToken(Long studentId) {
        return assembler.toDto(currentTokens(studentId));
    }

    /** The student's tokens, renewed first if they are about to expire. */
    SpotifyTokens currentTokens(Long studentId) {
        requireConfigured();
        SpotifyTokens current = tokens.find(studentId)
                .orElseThrow(() -> new SpotifyNotConnectedException("Connect Spotify to use it here"));
        if (!current.expiresWithin(properties.tokenRefreshMargin(), clock.instant())) {
            return current;
        }
        try {
            SpotifyTokens renewed = current.renewedWith(accounts.refresh(current.refreshToken()));
            tokens.save(studentId, renewed);
            return renewed;
        } catch (SpotifyNotConnectedException lapsed) {
            tokens.remove(studentId);
            throw lapsed;
        } catch (IOException failure) {
            LOG.warn("Spotify token could not be renewed: {}", failure.getMessage());
            throw new SpotifyUnavailableException("Spotify is unavailable right now; try again later");
        }
    }

    private void requireConfigured() {
        if (!properties.ready()) {
            throw new SpotifyUnavailableException("Spotify is not set up on this server");
        }
    }

    private static SpotifyConnectionOutcomeDto outcome(Outcome outcome) {
        return new SpotifyConnectionOutcomeDto(outcome.name().toLowerCase());
    }

    private String returnUrl(Outcome outcome) {
        return UriComponentsBuilder.fromUriString(properties.returnUrl())
                .queryParam("spotify", outcome.name().toLowerCase()).build().toUriString();
    }
}
