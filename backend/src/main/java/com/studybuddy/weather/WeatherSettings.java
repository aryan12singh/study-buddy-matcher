package com.studybuddy.weather;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Campus weather settings bound from {@code app.weather}: whether it is on, the location, the
 * request timeout, and how long a reading is cached, retried and still shown when stale. Startup
 * fails on values outside sensible bounds.
 */
@ConfigurationProperties("app.weather")
public record WeatherSettings(@DefaultValue("true") boolean enabled,
    @DefaultValue("1.297") double latitude, @DefaultValue("103.849") double longitude,
    @DefaultValue("SMU campus") String location,
    @DefaultValue("4s") Duration timeout, @DefaultValue("15m") Duration cacheLifetime,
    @DefaultValue("1m") Duration retryInterval, @DefaultValue("24h") Duration maximumStaleAge) {
    public WeatherSettings {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
            || !Double.isFinite(longitude) || longitude < -180 || longitude > 180
            || location == null || location.isBlank() || location.length() > 80
            || timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofSeconds(10)) > 0
            || cacheLifetime == null || cacheLifetime.compareTo(Duration.ofSeconds(30)) < 0
            || retryInterval == null || retryInterval.compareTo(Duration.ofSeconds(30)) < 0
            || maximumStaleAge == null || maximumStaleAge.compareTo(cacheLifetime) < 0) {
            throw new IllegalArgumentException("Invalid weather configuration");
        }
    }
}
