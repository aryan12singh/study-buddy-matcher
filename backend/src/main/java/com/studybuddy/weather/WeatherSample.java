package com.studybuddy.weather;

import java.time.Instant;

public record WeatherSample(WeatherCondition condition, double temperatureCelsius, boolean isDay, Instant observedAt) { }
