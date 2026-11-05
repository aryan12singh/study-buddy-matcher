package com.studybuddy.profile;

import com.studybuddy.security.AccountPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@code GET /api/students/{id}/profile}, another student's profile as the signed-in student
 * may see it. The response is marked {@code no-store} so no browser or proxy keeps a copy of a
 * contact number. It only delegates to {@link ProfileViewService}.
 */
@RestController
@RequestMapping("/api/students")
public class ProfileViewController {
    private final ProfileViewService service;

    public ProfileViewController(ProfileViewService service) {
        this.service = service;
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<ProfileDto> view(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.view(id, actor.id()));
    }
}
