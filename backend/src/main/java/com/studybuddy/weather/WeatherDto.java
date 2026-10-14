package com.studybuddy.weather;

import java.time.Instant;

public record WeatherDto(boolean available, WeatherCondition condition, Double temperatureCelsius,
    boolean isDay, Instant observedAt, boolean stale, String location) { }
