package com.studybuddy.matching;

import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    @GetMapping
    public MatchSearchResultDto search(@AuthenticationPrincipal AccountPrincipal actor,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) StudyGoal studyGoal,
            @RequestParam(required = false) StudyMode studyMode,
            @RequestParam(required = false) GroupSizePreference groupSize,
            @RequestParam(required = false) Integer minSharedHours,
            @RequestParam(required = false) MatchQuality minQuality,
            @RequestParam(required = false) MatchSort sort,
            @RequestParam(required = false) Integer limit) {
        return service.search(new MatchSearchCriteria(courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality, sort, limit), actor.id());
    }
}
