package com.studybuddy.studyplan;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Creates a study plan in three steps: a short database read that checks the request, the AI call,
 * and a short database write. Deliberately not transactional: Gemini can take many seconds, and a
 * transaction held open that long would tie up a database connection and the account lock that
 * account deletion waits on. A roadmap written in advance, such as the demo seed's, skips the AI
 * call but passes the same checks.
 */
@Service
public class StudyPlanCreationService {
    private final StudyPlanService plans;
    private final StudyMaterialReader materials;
    private final RoadmapWriter roadmaps;
    private final StudyPlanOutputValidator validator;

    public StudyPlanCreationService(StudyPlanService plans, StudyMaterialReader materials, RoadmapWriter roadmaps,
            StudyPlanOutputValidator validator) {
        this.plans = plans;
        this.materials = materials;
        this.roadmaps = roadmaps;
        this.validator = validator;
    }

    public StudyPlanDto createPersonal(Long actorId, PersonalPlanInput input, List<MultipartFile> files) {
        PersonalPlanRequest request = plans.preparePersonal(actorId, input);
        StudyMaterial material = materials.read(files, input.topicsText());
        RoadmapDraft draft = roadmaps.write(actorId, request.course(), request.goal(), request.frame(), material);
        return plans.savePersonal(actorId, request, material, draft);
    }

    /** Replaces the session's roadmap; the meetings and their length come from the session. */
    public StudyPlanDto createForSession(Long actorId, Long sessionId, SessionPlanInput input, List<MultipartFile> files) {
        SessionPlanRequest request = plans.prepareSession(actorId, sessionId, input);
        StudyMaterial material = materials.read(files, input.topicsText());
        RoadmapDraft draft = roadmaps.write(actorId, request.course(), request.goal(), request.frame(), material);
        return plans.saveSession(actorId, request, material, draft);
    }

    /** Creates a plan from pasted topics and a roadmap written in advance, in the shape of Gemini's reply. */
    public StudyPlanDto createPersonalPrewritten(Long actorId, PersonalPlanInput input, String roadmap) {
        PersonalPlanRequest request = plans.preparePersonal(actorId, input);
        StudyMaterial material = materials.read(List.of(), input.topicsText());
        RoadmapDraft draft = validator.validate(roadmap, request.frame())
                .orElseThrow(() -> new IllegalArgumentException("The prewritten roadmap does not fit this plan's weeks"));
        return plans.savePersonal(actorId, request, material, draft);
    }
}
