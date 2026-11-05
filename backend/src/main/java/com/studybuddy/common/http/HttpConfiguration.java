package com.studybuddy.common.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

/**
 * HTTP-level configuration: registers {@link HttpProperties} ({@code app.http}) and makes every
 * JSON string pass through {@link TextWithoutNulDeserializer}.
 */
@Configuration
@EnableConfigurationProperties(HttpProperties.class)
public class HttpConfiguration {

    /** Every String read from JSON goes through {@link TextWithoutNulDeserializer}. */
    @Bean
    JsonMapperBuilderCustomizer textWithoutNul() {
        return builder -> builder.addModule(new SimpleModule("text-without-nul")
            .addDeserializer(String.class, new TextWithoutNulDeserializer()));
    }
}
