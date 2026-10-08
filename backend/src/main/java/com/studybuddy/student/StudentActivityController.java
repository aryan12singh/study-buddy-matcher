package com.studybuddy.student;

import com.studybuddy.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/me")
public class StudentActivityController {
    private final StudentActivityService service;

    public StudentActivityController(StudentActivityService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public StudentActivityDto summary(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.summary(actor.id());
    }
}
