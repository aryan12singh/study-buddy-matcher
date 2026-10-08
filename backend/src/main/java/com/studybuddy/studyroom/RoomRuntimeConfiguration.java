package com.studybuddy.studyroom;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RoomSettings.class)
public class RoomRuntimeConfiguration {
    @Bean
    public Clock studyRoomClock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }
}
