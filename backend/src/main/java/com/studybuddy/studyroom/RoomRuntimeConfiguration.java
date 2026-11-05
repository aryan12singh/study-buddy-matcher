package com.studybuddy.studyroom;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Group room beans: registers {@link RoomSettings} ({@code app.study-room}) and
 * {@link RoomReactionSettings} ({@code app.study-room.reactions}), and supplies the UTC clock the
 * room timer and presence leases run on, which tests can replace.
 */
@Configuration
@EnableConfigurationProperties({RoomSettings.class, RoomReactionSettings.class})
public class RoomRuntimeConfiguration {
    @Bean
    public Clock studyRoomClock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }
}
