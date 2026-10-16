package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explains one match on request. Takes the same search context as {@code GET /api/matches}
 * so the explanation describes exactly the score the student saw.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchExplanationController {
    private final MatchExplanationService service;

    public MatchExplanationController(MatchExplanationService service) {
        this.service = service;
    }

    @GetMapping("/{candidateId}/explanation")
    public MatchExplanationDto explain(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable Long candidateId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) StudyGoal studyGoal,
            @RequestParam(required = false) StudyMode studyMode,
            @RequestParam(required = false) GroupSizePreference groupSize,
            @RequestParam(required = false) Integer minSharedHours,
            @RequestParam(required = false) MatchQuality minQuality) {
        return service.explain(new MatchSearchCriteria(courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality),
                actor.id(), candidateId);
    }
}
