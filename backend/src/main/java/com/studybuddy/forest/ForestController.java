package com.studybuddy.forest;

import com.studybuddy.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forest/me")
public class ForestController {
    private final ForestService service;

    public ForestController(ForestService service) {
        this.service = service;
    }

    @GetMapping
    public ForestDto mine(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.forestOf(actor.id());
    }

    @GetMapping("/rooms/{groupId}")
    public GrowingTreeDto growing(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.growingIn(actor.id(), groupId);
    }
}
