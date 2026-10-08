package com.studybuddy.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Opt-in only. Runtime passwords and synthetic data never enter logs. */
@Component
@EnableConfigurationProperties(DemoSeedProperties.class)
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoSeedBootstrap implements ApplicationRunner {
    private final DemoSeedService service;

    public DemoSeedBootstrap(DemoSeedService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        service.seed();
    }
}
