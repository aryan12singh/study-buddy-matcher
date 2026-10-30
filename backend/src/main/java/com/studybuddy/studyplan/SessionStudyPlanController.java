package com.studybuddy.studyplan;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** A study session's roadmap. Ticking and deleting it use the plan's own {@code /api/study-plans} paths. */
@RestController
@RequestMapping("/api/sessions/{sessionId}/study-plan")
public class SessionStudyPlanController {
    private final StudyPlanCreationService creation;
    private final StudyPlanService plans;

    public SessionStudyPlanController(StudyPlanCreationService creation, StudyPlanService plans) {
        this.creation = creation;
        this.plans = plans;
    }

    /** Multipart: the input's fields as form fields, plus zero or more parts named {@code files}. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StudyPlanDto create(@PathVariable Long sessionId, @AuthenticationPrincipal AccountPrincipal actor,
            @Valid @ModelAttribute SessionPlanInput input, @RequestPart(name = "files", required = false) List<MultipartFile> files) {
        return creation.createForSession(actor.id(), sessionId, input, files);
    }

    /** 204 No Content when the session has no roadmap yet. */
    @GetMapping
    public ResponseEntity<StudyPlanDto> get(@PathVariable Long sessionId, @AuthenticationPrincipal AccountPrincipal actor) {
        return plans.getForSession(sessionId, actor.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
