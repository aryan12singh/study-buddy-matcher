package com.studybuddy.course;

import com.studybuddy.security.AccountPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@code GET /api/courses}, the course catalogue in code order. It only delegates to
 * {@link CourseService}.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @GetMapping
    public List<CourseDto> list(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.list(actor.id());
    }
}
