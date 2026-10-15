package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingProfile;

/** One candidate in a search, with their score against the searcher. */
record ScoredCandidate(MatchingProfile profile, MatchScore score) {
}
