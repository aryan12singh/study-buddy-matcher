package com.studybuddy.matchexplanation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caches generated match explanations by a hash of the evidence they were written from, so the same
 * evidence never calls the AI provider twice.
 */
public interface MatchExplanationRepository extends JpaRepository<MatchExplanation, Long> {
    Optional<MatchExplanation> findByEvidenceHash(String evidenceHash);

    /** Two students clicking at once may both write the same evidence; the second insert is ignored. */
    @Transactional
    @Modifying
    @Query(value = "insert into match_explanations(evidence_hash,prompt_version,model,explanation_text,created_at) "
            + "values (:hash,:promptVersion,:model,:text,now()) on conflict(evidence_hash) do nothing", nativeQuery = true)
    void insertIfAbsent(@Param("hash") String hash, @Param("promptVersion") String promptVersion,
            @Param("model") String model, @Param("text") String text);
}
