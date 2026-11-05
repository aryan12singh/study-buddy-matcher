package com.studybuddy.weather;

import java.time.Instant;

/**
 * The campus weather shown in the Study Room: condition, temperature, day or night, when it was
 * observed, and whether the latest refresh failed so an older reading is being shown.
 */
public record WeatherDto(boolean available, WeatherCondition condition, Double temperatureCelsius,
    boolean isDay, Instant observedAt, boolean stale, String location) { }
