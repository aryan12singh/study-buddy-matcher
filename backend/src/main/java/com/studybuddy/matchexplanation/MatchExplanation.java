package com.studybuddy.matchexplanation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/** One cached explanation. Never leaves the service layer; the API sees only its text. */
@Entity
@Table(name = "match_explanations", uniqueConstraints =
        @UniqueConstraint(name = "match_explanations_evidence_hash_key", columnNames = "evidence_hash"))
public class MatchExplanation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_hash", nullable = false, length = 64)
    private String evidenceHash;

    @Column(name = "prompt_version", nullable = false, length = 16)
    private String promptVersion;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "explanation_text", nullable = false, columnDefinition = "text")
    private String explanationText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MatchExplanation() {
    }

    public String getExplanationText() {
        return explanationText;
    }
}
