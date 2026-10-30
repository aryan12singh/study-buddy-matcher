package com.studybuddy.music;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpotifyConnectionServiceTest {
    private final Instant now = Instant.parse("2026-10-22T08:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final SpotifyAuthorizationStore authorizations = mock(SpotifyAuthorizationStore.class);
    private final SpotifyTokenStore tokens = new SpotifyTokenStore();
    private final SpotifyAccountsClient accounts = mock(SpotifyAccountsClient.class);

    private SpotifyConnectionService service(SpotifyProperties properties) {
        return new SpotifyConnectionService(properties, authorizations, tokens, accounts, new SpotifyAssembler(), clock);
    }

    private final SpotifyConnectionService service = service(SpotifyTestSettings.configured());

    @Test void statusReflectsConfigurationAndConnection() {
        assertEquals(new SpotifyStatusDto(false, false, "3bZTQGrvewbkJJe6UpTFKc"), service(SpotifyTestSettings.withCredentials("", "")).status(7L));
        assertEquals(new SpotifyStatusDto(true, false, "3bZTQGrvewbkJJe6UpTFKc"), service.status(7L));
        tokens.save(7L, new SpotifyTokens("access", "refresh", now.plusSeconds(3600)));
        assertEquals(new SpotifyStatusDto(true, true, "3bZTQGrvewbkJJe6UpTFKc"), service.status(7L));
    }

    @Test void authorizationUrlCarriesClientRedirectScopesAndState() {
        when(authorizations.start(7L)).thenReturn("state-1");
        String url = service.startAuthorization(7L).url();
        assertTrue(url.startsWith("https://accounts.spotify.com/authorize?"));
        assertTrue(url.contains("client_id=client"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("redirect_uri=http://127.0.0.1:8080/api/spotify/callback"));
        assertTrue(url.contains("scope=streaming%20user-read-email"));
        assertTrue(url.contains("state=state-1"));
        assertFalse(url.contains("secret"));
    }

    @Test void unconfiguredServerCannotStartSignIn() {
        assertThrows(SpotifyUnavailableException.class, () -> service(SpotifyTestSettings.withCredentials("client", " ")).startAuthorization(7L));
    }

    @Test void successfulCallbackSavesTokensForTheStudentWhoStarted() throws Exception {
        when(authorizations.finish("state-1")).thenReturn(Optional.of(7L));
        when(accounts.exchangeCode("code-1")).thenReturn(new SpotifyTokens("access", "refresh", now.plusSeconds(3600)));
        assertEquals("http://localhost:5173/study-room?spotify=connected", service.completeAuthorization("state-1", "code-1", null));
        assertEquals("access", tokens.find(7L).orElseThrow().accessToken());
    }

    @Test void unknownStateDeniedAccessAndProviderFailureEachReturnToMusic() throws Exception {
        when(authorizations.finish("forged")).thenReturn(Optional.empty());
        when(authorizations.finish("denied")).thenReturn(Optional.of(7L));
        when(authorizations.finish("failing")).thenReturn(Optional.of(7L));
        when(accounts.exchangeCode("code-2")).thenThrow(new IOException("down"));
        assertEquals("http://localhost:5173/study-room?spotify=expired", service.completeAuthorization("forged", "code-1", null));
        assertEquals("http://localhost:5173/study-room?spotify=denied", service.completeAuthorization("denied", null, "access_denied"));
        assertEquals("http://localhost:5173/study-room?spotify=failed", service.completeAuthorization("failing", "code-2", null));
        verify(accounts, never()).exchangeCode("code-1");
        assertTrue(tokens.find(7L).isEmpty());
    }

    @Test void freshTokenIsReturnedWithoutRenewing() throws Exception {
        tokens.save(7L, new SpotifyTokens("access", "refresh", now.plusSeconds(3600)));
        assertEquals("access", service.accessToken(7L).accessToken());
        verify(accounts, never()).refresh(any());
    }

    @Test void tokenAboutToExpireIsRenewedKeepingTheRefreshToken() throws Exception {
        tokens.save(7L, new SpotifyTokens("old", "refresh", now.plusSeconds(30)));
        when(accounts.refresh("refresh")).thenReturn(new SpotifyTokens("new", null, now.plusSeconds(3600)));
        assertEquals("new", service.accessToken(7L).accessToken());
        assertEquals(new SpotifyTokens("new", "refresh", now.plusSeconds(3600)).refreshToken(), tokens.find(7L).orElseThrow().refreshToken());
    }

    @Test void rejectedRenewalDisconnectsTheStudent() throws Exception {
        tokens.save(7L, new SpotifyTokens("old", "refresh", now));
        when(accounts.refresh("refresh")).thenThrow(new SpotifyNotConnectedException("expired"));
        assertThrows(SpotifyNotConnectedException.class, () -> service.accessToken(7L));
        assertTrue(tokens.find(7L).isEmpty());
    }

    @Test void notConnectedStudentHasNoToken() {
        assertThrows(SpotifyNotConnectedException.class, () -> service.accessToken(7L));
    }
}
