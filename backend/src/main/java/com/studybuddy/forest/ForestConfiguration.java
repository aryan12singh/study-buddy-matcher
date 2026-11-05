package com.studybuddy.forest;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Forest beans: binds {@link ForestProperties} ({@code app.forest}), supplies the random source for
 * tree variants and plots, and the single-threaded executor that applies forest updates.
 */
@Configuration
@EnableConfigurationProperties(ForestProperties.class)
public class ForestConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ForestConfiguration.class);

    @Bean
    public RandomGenerator forestRandom() {
        return new SecureRandom();
    }

    /**
     * One worker applies forest updates in the order rooms commit them. Running them here, after the
     * room request has returned its database connection, means a busy room never holds two pooled
     * connections at once. When the queue is full an update is dropped and logged; trees are a reward,
     * so a missed tick only costs a couple of seconds of credit.
     */
    @Bean
    public ThreadPoolTaskExecutor forestUpdates(ForestProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(properties.updateQueueCapacity());
        executor.setThreadNamePrefix("forest-");
        executor.setRejectedExecutionHandler((update, pool) -> LOG.warn("Forest update queue is full; dropped one update"));
        return executor;
    }
}
