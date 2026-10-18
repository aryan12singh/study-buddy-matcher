package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The cache key for an explanation. Any change to the evidence, the prompt version or the model
 * produces a different key. This semantic identity deliberately excludes student identity and
 * exact card inputs; matching separately verifies the card fingerprint before any cache lookup.
 */
@Component
public class ExplanationHasher {
    private final JsonMapper json;

    public ExplanationHasher(JsonMapper json) {
        this.json = json;
    }

    public String hash(ExplanationEvidence evidence, String promptVersion, String model) {
        String canonical = json.writeValueAsString(evidence) + "\n" + promptVersion + "\n" + model;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Java runtime provides SHA-256", impossible);
        }
    }
}
