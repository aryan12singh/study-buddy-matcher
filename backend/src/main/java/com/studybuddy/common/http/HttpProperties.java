package com.studybuddy.common.http;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Request limits that apply to every endpoint, bound from {@code app.http.*}. File uploads are
 * multipart, not JSON, so they keep the separate {@code spring.servlet.multipart} limits.
 */
@ConfigurationProperties("app.http")
public record HttpProperties(DataSize maxJsonBodySize) {
    public HttpProperties {
        Objects.requireNonNull(maxJsonBodySize, "The JSON body size limit is required");
        // JsonBodySizeFilter buffers a body of unknown length in one array, so the limit must fit in one.
        if (maxJsonBodySize.toBytes() < 1 || maxJsonBodySize.toBytes() >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The JSON body size limit must be at least 1 byte and under 2 GB");
        }
    }
}
