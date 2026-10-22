package com.studybuddy.music;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Uses the existing authenticated-student API guard. */
@RestController
@RequestMapping("/api/ambient")
public class AmbientController {
    private final AmbientSoundService service;

    public AmbientController(AmbientSoundService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public List<AmbientCategoryDto> categories() {
        return service.categories();
    }

    @GetMapping("/categories/{categoryId}/sound")
    public AmbientSoundDto randomSound(@PathVariable String categoryId, @RequestParam(required = false) Long exclude) {
        return service.randomSound(categoryId, exclude);
    }
}
