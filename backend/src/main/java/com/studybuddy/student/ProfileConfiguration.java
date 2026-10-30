package com.studybuddy.student;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link ProfileProperties} ({@code app.profile}) and {@link AvailabilityProperties}
 * ({@code app.availability}).
 */
@Configuration
@EnableConfigurationProperties({ProfileProperties.class, AvailabilityProperties.class})
public class ProfileConfiguration {
}
