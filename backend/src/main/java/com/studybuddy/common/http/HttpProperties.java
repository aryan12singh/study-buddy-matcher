package com.studybuddy.common.http;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Request limits that apply to every endpoint, bound from {@code app.http.*}. File uploads are
 * multipart, not JSON, so they keep the separate {@code spring.servlet.multipart} limits.
 */
@Validated
@ConfigurationProperties("app.http")
public record HttpProperties(@NotNull DataSize maxJsonBodySize) {
}
