package com.studybuddy.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

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
     * throws: the student always lands back on the Study Room with an outcome to show.
     */
    public String completeAuthorization(String state, String code, String error) {
        Optional<Long> studentId = authorizations.finish(state);
        if (studentId.isEmpty() || !properties.ready()) {
            return returnUrl(Outcome.EXPIRED);
        }
        if (error != null || code == null || code.isBlank()) {
            return returnUrl(Outcome.DENIED);
        }
        try {
            tokens.save(studentId.get(), accounts.exchangeCode(code));
            return returnUrl(Outcome.CONNECTED);
        } catch (IOException failure) {
            LOG.warn("Spotify sign-in could not be completed: {}", failure.getMessage());
            return returnUrl(Outcome.FAILED);
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

    public void disconnect(Long studentId) {
        tokens.remove(studentId);
    }

    private void requireConfigured() {
        if (!properties.ready()) {
            throw new SpotifyUnavailableException("Spotify is not set up on this server");
        }
    }

    private String returnUrl(Outcome outcome) {
        return UriComponentsBuilder.fromUriString(properties.returnUrl())
                .queryParam("spotify", outcome.name().toLowerCase()).build().toUriString();
    }
}
