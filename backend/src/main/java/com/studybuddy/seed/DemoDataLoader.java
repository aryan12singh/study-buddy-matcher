package com.studybuddy.seed;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Reads the demo dataset bundled with the application. */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoDataLoader {

    private static final String LOCATION = "demo-seed/demo-data.json";

    private final JsonMapper json;

    public DemoDataLoader(JsonMapper json) {
        this.json = json;
    }

    public DemoData load() {
        try (InputStream input = new ClassPathResource(LOCATION).getInputStream()) {
            return json.readValue(input, DemoData.class);
        } catch (IOException error) {
            throw new UncheckedIOException("Could not read the demo dataset " + LOCATION, error);
        }
    }
}
