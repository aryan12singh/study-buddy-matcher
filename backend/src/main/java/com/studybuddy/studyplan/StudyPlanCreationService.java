package com.studybuddy.studyplan;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Creates a study plan in three steps: a short database read that checks the request, the AI call,
 * and a short database write. Deliberately not transactional: Gemini can take many seconds, and a
 * transaction held open that long would tie up a database connection and the account lock that
 * account deletion waits on.
 */
@Service
public class StudyPlanCreationService {
    private final StudyPlanService plans;
    private final StudyMaterialReader materials;
    private final RoadmapWriter roadmaps;

    public StudyPlanCreationService(StudyPlanService plans, StudyMaterialReader materials, RoadmapWriter roadmaps) {
        this.plans = plans;
        this.materials = materials;
        this.roadmaps = roadmaps;
    }

    public StudyPlanDto createPersonal(Long actorId, PersonalPlanInput input, List<MultipartFile> files) {
        PersonalPlanRequest request = plans.preparePersonal(actorId, input);
        StudyMaterial material = materials.read(files, input.topicsText());
        RoadmapDraft draft = roadmaps.write(actorId, request.course(), request.goal(), request.frame(), material);
        return plans.savePersonal(actorId, request, material, draft);
    }
}
