package com.studybuddy.weather;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link WeatherSettings}, bound from {@code app.weather}. */
@Configuration
@EnableConfigurationProperties(WeatherSettings.class)
public class WeatherConfiguration { }
