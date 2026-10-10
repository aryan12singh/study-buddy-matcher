package com.studybuddy.matching.config;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/matching-config")
public class AdminMatchingConfigController {

    private final MatchingConfigService service;

    public AdminMatchingConfigController(MatchingConfigService service) {
        this.service = service;
    }

    @GetMapping
    public MatchingConfigDto get(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.get(actor.id());
    }

    @PutMapping
    public MatchingConfigDto update(@AuthenticationPrincipal AccountPrincipal actor,
            @Valid @RequestBody UpdateMatchingConfigRequest request) {
        return service.update(request, actor.id());
    }
}
