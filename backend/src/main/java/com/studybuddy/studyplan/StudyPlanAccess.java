package com.studybuddy.studyplan;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Who may see and change a plan. A personal plan belongs to its student alone. A plan the actor
 * may not see is reported as not found, so its ID reveals nothing. Session plans are readable by
 * nobody until the session plan rules (session participants and managers) are added with their
 * endpoints.
 */
@Component
public class StudyPlanAccess {

    public void requireReadable(StudyPlan plan, Long actorId) {
        if (!isOwner(plan, actorId)) {
            throw new StudyPlanNotFoundException(plan.getId());
        }
    }

    public void requireManageable(StudyPlan plan, Long actorId) {
        if (!canManage(plan, actorId)) {
            throw new StudyPlanNotFoundException(plan.getId());
        }
    }

    public boolean canManage(StudyPlan plan, Long actorId) {
        return isOwner(plan, actorId);
    }

    private static boolean isOwner(StudyPlan plan, Long actorId) {
        return plan.getScope() == StudyPlanScope.PERSONAL && Objects.equals(plan.getStudent().getId(), actorId);
    }
}
