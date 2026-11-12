package com.studybuddy.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** How {@code app.ai.primary-provider} is read at startup. */
class AiProviderPropertiesTest {
    @Configuration
    @EnableConfigurationProperties(AiProviderProperties.class)
    static class Binding {
    }

    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(Binding.class);

    @Test
    void readsEitherProviderInAnyCase() {
        context.withPropertyValues("app.ai.primary-provider=gemini").run(started ->
                assertEquals(AiProvider.GEMINI, started.getBean(AiProviderProperties.class).primaryProvider()));
        context.withPropertyValues("app.ai.primary-provider=Claude").run(started ->
                assertEquals(AiProvider.CLAUDE, started.getBean(AiProviderProperties.class).primaryProvider()));
    }

    @Test
    void stopsStartupForAnUnknownProvider() {
        context.withPropertyValues("app.ai.primary-provider=chatgpt").run(started -> assertNotNull(started.getStartupFailure()));
    }

    @Test
    void stopsStartupWhenNoProviderIsNamed() {
        context.run(started -> assertNotNull(started.getStartupFailure()));
        context.withPropertyValues("app.ai.primary-provider=gemini").run(started -> assertNull(started.getStartupFailure()));
    }
}
