package com.studybuddy.music;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code app.spotify.*}, the same way the other feature settings are bound. */
@Configuration
@EnableConfigurationProperties(SpotifyProperties.class)
public class SpotifyConfiguration {
}
