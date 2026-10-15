package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;

import java.util.Set;

/**
 * Turns a scored match into a short sentence a student can read. It receives only the
 * score, never a student's profile, so an explainer cannot reveal private details.
 * Separate from scoring so the wording can change, or be generated differently,
 * without touching how matches are scored.
 */
public interface MatchExplainer {

    String explain(MatchScore score, Set<MatchingCriterion> enabled);
}
