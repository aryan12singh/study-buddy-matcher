package com.studybuddy.weather;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherServiceTest {
    private final Instant now = Instant.parse("2026-10-14T08:00:00Z");
    private final WeatherSettings settings = new WeatherSettings(true, 1.297, 103.849, "SMU campus",
        Duration.ofSeconds(4), Duration.ofMinutes(15), Duration.ofMinutes(1), Duration.ofHours(24));

    @Test void cacheIsSharedAndFailedFetchKeepsBoundedLastKnownConditions() throws Exception {
        WeatherClient client = mock(WeatherClient.class);
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now, now.plusSeconds(30), now.plusSeconds(901), now.plusSeconds(902), now.plusSeconds(86401));
        when(client.fetch()).thenReturn(new WeatherSample(WeatherCondition.RAIN, 28, true, now))
            .thenThrow(new IOException("Unavailable"));
        var service = new WeatherService(client, settings, new WeatherAssembler(), clock);
        assertTrue(service.current().available());
        assertFalse(service.current().stale());
        verify(client, times(1)).fetch();
        assertTrue(service.current().stale());
        assertTrue(service.current().available());
        verify(client, times(2)).fetch();
        assertFalse(service.current().available());
    }

    @Test void slowProviderDoesNotBlockOtherReaders() throws Exception {
        WeatherClient client = mock(WeatherClient.class);
        var fetching = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(client.fetch()).thenAnswer(call -> {
            fetching.countDown();
            release.await();
            return new WeatherSample(WeatherCondition.CLEAR, 30, true, now);
        });
        var service = new WeatherService(client, settings, new WeatherAssembler(), Clock.fixed(now, java.time.ZoneOffset.UTC));
        var slow = CompletableFuture.supplyAsync(service::current);
        try {
            assertTrue(fetching.await(5, TimeUnit.SECONDS));
            var other = CompletableFuture.supplyAsync(service::current).get(2, TimeUnit.SECONDS);
            assertFalse(other.available(), "Readers see the cache while the provider is slow");
        } finally {
            release.countDown();
        }
        assertTrue(slow.get(5, TimeUnit.SECONDS).available());
        verify(client, times(1)).fetch();
    }

    @Test void disabledWeatherMakesNoProviderRequest() {
        WeatherClient client = mock(WeatherClient.class);
        var disabled = new WeatherSettings(false, settings.latitude(), settings.longitude(), settings.location(),
            settings.timeout(), settings.cacheLifetime(), settings.retryInterval(), settings.maximumStaleAge());
        var service = new WeatherService(client, disabled, new WeatherAssembler(), Clock.fixed(now, java.time.ZoneOffset.UTC));
        assertFalse(service.current().available());
        verifyNoInteractions(client);
    }

    @Test void parserRejectsMissingUnknownOrMalformedProviderDataAndMapsPublishedCodes() throws Exception {
        var sample = OpenMeteoClient.parse("{\"current\":{\"weather_code\":97,\"temperature_2m\":27.5,\"is_day\":0,\"time\":1791964800}}");
        assertEquals(WeatherCondition.STORM, sample.condition());
        assertFalse(sample.isDay());
        assertThrows(IOException.class, () -> OpenMeteoClient.parse("{\"current\":{}}"));
        assertThrows(IOException.class, () -> OpenMeteoClient.parse("{\"current\":{\"weather_code\":123,\"temperature_2m\":27,\"is_day\":1,\"time\":1791964800}}"));
        assertThrows(IOException.class, () -> OpenMeteoClient.parse("not-json"));
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmo(85));
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromWmo(48));
    }

    @Test void failedInitialFetchBacksOffAndFallsBackWithoutInventingLiveConditions() throws Exception {
        WeatherClient client = mock(WeatherClient.class);
        when(client.fetch()).thenThrow(new IOException("Unavailable"));
        var service = new WeatherService(client, settings, new WeatherAssembler(), Clock.fixed(now, java.time.ZoneOffset.UTC));
        assertFalse(service.current().available());
        assertNull(service.current().observedAt());
        verify(client, times(1)).fetch();
    }
}
