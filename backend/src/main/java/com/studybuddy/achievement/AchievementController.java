package com.studybuddy.achievement;

import com.studybuddy.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@code GET /api/achievements/me}, the signed-in student's level, points, streak and
 * badges. It only delegates to {@link AchievementService}.
 */
@RestController
@RequestMapping("/api/achievements")
public class AchievementController {
    private final AchievementService service;

    public AchievementController(AchievementService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public AchievementsDto mine(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.forStudent(actor.id());
    }
}
