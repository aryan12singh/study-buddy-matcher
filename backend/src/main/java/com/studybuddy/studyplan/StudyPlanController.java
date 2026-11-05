package com.studybuddy.studyplan;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Personal study plan endpoints under {@code /api/study-plans}: create one from notes and files,
 * list, read, mark an entry done or not done, and delete. Creating delegates to
 * {@link StudyPlanCreationService}, which calls the AI provider outside any transaction; everything
 * else delegates to {@link StudyPlanService}.
 */
@RestController
@RequestMapping("/api/study-plans")
public class StudyPlanController {
    private final StudyPlanCreationService creation;
    private final StudyPlanService plans;

    public StudyPlanController(StudyPlanCreationService creation, StudyPlanService plans) {
        this.creation = creation;
        this.plans = plans;
    }

    /** Multipart: the input's fields as form fields, plus zero or more parts named {@code files}. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StudyPlanDto create(@AuthenticationPrincipal AccountPrincipal actor, @Valid @ModelAttribute PersonalPlanInput input,
            @RequestPart(name = "files", required = false) List<MultipartFile> files) {
        return creation.createPersonal(actor.id(), input, files);
    }

    @GetMapping
    public List<StudyPlanSummaryDto> mine(@AuthenticationPrincipal AccountPrincipal actor) {
        return plans.listMine(actor.id());
    }

    @GetMapping("/{planId}")
    public StudyPlanDto get(@PathVariable Long planId, @AuthenticationPrincipal AccountPrincipal actor) {
        return plans.get(planId, actor.id());
    }

    @PutMapping("/{planId}/entries/{entryId}/progress")
    public StudyPlanDto markEntry(@PathVariable Long planId, @PathVariable Long entryId,
            @AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody StudyPlanProgressInput input) {
        return plans.markEntry(planId, entryId, actor.id(), input);
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long planId, @AuthenticationPrincipal AccountPrincipal actor) {
        plans.delete(planId, actor.id());
    }
}
