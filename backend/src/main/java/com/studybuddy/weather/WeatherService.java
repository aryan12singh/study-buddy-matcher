package com.studybuddy.weather;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** One bounded, shared cache prevents individual room polls from hitting the provider. */
@Service
public class WeatherService {
    /** Tolerated provider clock drift before an observation counts as from the future. */
    private static final Duration MAX_PROVIDER_CLOCK_SKEW = Duration.ofMinutes(5);

    private final WeatherClient client;
    private final WeatherSettings settings;
    private final WeatherAssembler assembler;
    private final Clock clock;
    private WeatherSample lastGood;
    private Instant nextAttempt = Instant.MIN;
    private boolean failed;

    public WeatherService(WeatherClient client, WeatherSettings settings, WeatherAssembler assembler,
        @Qualifier("studyRoomClock") Clock clock) {
        this.client = client; this.settings = settings; this.assembler = assembler; this.clock = clock;
    }

    /** Only the caller that claims the refresh waits for the provider; everyone else reads the cache. */
    public WeatherDto current() {
        Instant now = clock.instant();
        if (!settings.enabled()) return assembler.toDto(null, false, settings.location());
        if (claimRefresh(now)) refresh(now);
        return snapshot(now);
    }

    private synchronized boolean claimRefresh(Instant now) {
        if (now.isBefore(nextAttempt)) return false;
        nextAttempt = now.plus(settings.retryInterval());
        return true;
    }

    private void refresh(Instant now) {
        try {
            WeatherSample sample = client.fetch();
            if (sample.observedAt().isAfter(now.plus(MAX_PROVIDER_CLOCK_SKEW))
                || !sample.observedAt().isAfter(now.minus(settings.maximumStaleAge()))) {
                throw new IOException("Weather observation is outside its validity window");
            }
            recordSuccess(sample, now);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            recordFailure();
        } catch (IOException | IllegalArgumentException unavailable) {
            recordFailure();
        }
    }

    private synchronized void recordSuccess(WeatherSample sample, Instant now) {
        lastGood = sample;
        failed = false;
        nextAttempt = now.plus(settings.cacheLifetime());
    }

    private synchronized void recordFailure() {
        failed = true;
    }

    private synchronized WeatherDto snapshot(Instant now) {
        WeatherSample usable = lastGood != null && lastGood.observedAt().isAfter(now.minus(settings.maximumStaleAge())) ? lastGood : null;
        return assembler.toDto(usable, failed, settings.location());
    }
}
