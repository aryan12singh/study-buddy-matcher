package com.studybuddy.student;

import com.studybuddy.security.AccountPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in student's own profile. Viewing another student's profile is a separate endpoint. */
@RestController
@RequestMapping("/api/profile/me")
public class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public MyProfileDto get(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.getMine(actor.id());
    }

    @PutMapping
    public MyProfileDto update(@AuthenticationPrincipal AccountPrincipal actor, @RequestBody UpdateProfileRequest request) {
        return service.updateMine(actor.id(), request);
    }

    @PutMapping("/availability")
    public List<AvailabilitySlotDto> replaceAvailability(@AuthenticationPrincipal AccountPrincipal actor,
        @RequestBody ReplaceAvailabilityRequest request) {
        return service.replaceAvailability(actor.id(), request);
    }
}
