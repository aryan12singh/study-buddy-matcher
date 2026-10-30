package com.studybuddy.weather;

import org.springframework.stereotype.Component;

/**
 * Builds the {@link WeatherDto}, marking it unavailable when there is no reading recent enough to
 * show.
 */
@Component
public class WeatherAssembler {
    public WeatherDto toDto(WeatherSample sample, boolean stale, String location) {
        return sample == null ? new WeatherDto(false, WeatherCondition.CLEAR, null, true, null, false, location)
            : new WeatherDto(true, sample.condition(), sample.temperatureCelsius(), sample.isDay(), sample.observedAt(), stale, location);
    }
}
