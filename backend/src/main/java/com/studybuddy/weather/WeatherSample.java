package com.studybuddy.weather;

import java.time.Instant;

/** One weather reading from the provider. */
public record WeatherSample(WeatherCondition condition, double temperatureCelsius, boolean isDay, Instant observedAt) { }
