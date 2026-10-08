package com.studybuddy.studygroup;

import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class StudyGroupController {
    private final StudyGroupService service;

    public StudyGroupController(StudyGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<StudyGroupSummaryDto> browse(@AuthenticationPrincipal AccountPrincipal actor,
        @RequestParam(required = false) Long courseId, @RequestParam(required = false) StudyGoal studyGoal,
        @RequestParam(required = false) StudyMode studyMode) {
        return service.browse(new StudyGroupFilter(courseId, studyGoal, studyMode), actor.id());
    }

    @GetMapping("/mine")
    public List<StudyGroupSummaryDto> mine(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.mine(actor.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupDetailDto create(@AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody StudyGroupDetails details) {
        return service.create(actor.id(), details);
    }

    @GetMapping("/{id}")
    public StudyGroupDetailDto get(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.get(id, actor.id());
    }

    @PutMapping("/{id}")
    public StudyGroupDetailDto update(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody StudyGroupDetails details) {
        return service.update(id, actor.id(), details);
    }

    @PostMapping("/{id}/close")
    public StudyGroupDetailDto close(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.close(id, actor.id());
    }

    @DeleteMapping("/{id}/members/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @PathVariable Long studentId, @AuthenticationPrincipal AccountPrincipal actor) {
        service.removeMember(id, actor.id(), studentId);
    }
}
