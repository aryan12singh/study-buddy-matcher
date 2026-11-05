package com.studybuddy.ai;

import com.studybuddy.common.error.InternalInvariantException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A versioned prompt read from the classpath, with {@code {{name}}} slots filled in one pass over
 * the template only. A value is never searched for slots, so whatever a student writes (even
 * "{{course}}") reaches the AI exactly as typed. A slot without a value is a fault in the
 * application and fails loudly rather than sending a half-filled prompt.
 */
public final class PromptTemplate {
    private static final Pattern SLOT = Pattern.compile("\\{\\{(\\w+)}}");

    private final String name;
    private final String text;

    private PromptTemplate(String name, String text) {
        this.name = name;
        this.text = text;
    }

    /**
     * Git on Windows may check a template out with \r\n line endings; they are made \n so the
     * prompt is the same everywhere.
     */
    public static PromptTemplate load(String classpathPath) {
        try (InputStream input = new ClassPathResource(classpathPath).getInputStream()) {
            return of(classpathPath, StreamUtils.copyToString(input, StandardCharsets.UTF_8));
        } catch (IOException missing) {
            throw new InternalInvariantException("Prompt template " + classpathPath + " is missing", missing);
        }
    }

    /** A template from text already in hand; {@code name} only appears in error messages. */
    public static PromptTemplate of(String name, String text) {
        return new PromptTemplate(name, text.replace("\r\n", "\n"));
    }

    /**
     * @throws InternalInvariantException when the template names a slot that has no value
     */
    public String fill(Map<String, String> values) {
        return SLOT.matcher(text).replaceAll(slot -> {
            String value = values.get(slot.group(1));
            if (value == null) {
                throw new InternalInvariantException("Prompt template " + name + " slot {{" + slot.group(1) + "}} has no value");
            }
            return Matcher.quoteReplacement(value);
        });
    }
}
