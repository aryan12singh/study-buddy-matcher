package com.studybuddy.ai;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Which AI provider every AI feature asks first, bound from {@code app.ai.primary-provider}. A value
 * that is not a provider's name stops the application at startup with a binding error.
 */
@Validated
@ConfigurationProperties("app.ai")
public record AiProviderProperties(@NotNull AiProvider primaryProvider) {
}
