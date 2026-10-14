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

    public synchronized WeatherDto current() {
        Instant now = clock.instant();
        if (!settings.enabled()) return assembler.toDto(null, false, settings.location());
        if (!now.isBefore(nextAttempt)) {
            nextAttempt = now.plus(settings.retryInterval());
            try {
                WeatherSample sample = client.fetch();
                if (sample.observedAt().isAfter(now.plus(Duration.ofMinutes(5)))
                    || !sample.observedAt().isAfter(now.minus(settings.maximumStaleAge()))) {
                    throw new IOException("Weather observation is outside its validity window");
                }
                lastGood = sample;
                failed = false;
                nextAttempt = now.plus(settings.cacheLifetime());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                failed = true;
            } catch (IOException | IllegalArgumentException unavailable) {
                failed = true;
            }
        }
        WeatherSample usable = lastGood != null && lastGood.observedAt().isAfter(now.minus(settings.maximumStaleAge())) ? lastGood : null;
        return assembler.toDto(usable, failed, settings.location());
    }
}
