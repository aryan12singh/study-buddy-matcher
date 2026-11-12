package com.studybuddy.ai;

import com.anthropic.client.AnthropicClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** When the application builds its one connection to Claude, and that Spring closes it on shutdown. */
class ClaudeConfigurationTest {
    @Configuration
    @EnableConfigurationProperties(ClaudeProperties.class)
    @Import(ClaudeConfiguration.class)
    static class Wiring {
    }

    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(Wiring.class)
            .withPropertyValues("app.ai.claude.base-url=http://127.0.0.1:9", "app.ai.claude.model=test-model",
                    "app.ai.claude.effort=high", "app.ai.claude.max-tokens=8000", "app.ai.claude.study-tools-max-tokens=16000",
                    "app.ai.claude.timeout=2s", "app.ai.claude.daily-call-limit=10",
                    "app.ai.claude.daily-call-limit-per-student=10", "app.ai.claude.quota-zone=UTC",
                    "app.ai.claude.rate-limit-cooldown=60s");

    @Test
    void startsWithoutAConnectionWhenClaudeIsSwitchedOffOrHasNoKey() {
        context.withPropertyValues("app.ai.claude.enabled=false", "app.ai.claude.api-key=unused-key").run(started -> {
            assertNull(started.getStartupFailure());
            assertTrue(started.getBeansOfType(AnthropicClient.class).isEmpty());
        });
        context.withPropertyValues("app.ai.claude.enabled=true", "app.ai.claude.api-key=").run(started -> {
            assertNull(started.getStartupFailure());
            assertTrue(started.getBeansOfType(AnthropicClient.class).isEmpty());
        });
    }

    @Test
    void buildsOneConnectionThatSpringClosesOnShutdown() {
        context.withPropertyValues("app.ai.claude.enabled=true", "app.ai.claude.api-key=test-key").run(started -> {
            assertNull(started.getStartupFailure());
            assertEquals(1, started.getBeansOfType(AnthropicClient.class).size());
            assertEquals("close", started.getBeanFactory().getBeanDefinition("anthropicClient").getDestroyMethodName());
        });
    }
}
