package com.studybuddy.studygroup;

import org.springframework.stereotype.Component;

/**
 * Converts {@link GroupJoinRequest} entities into {@link GroupJoinRequestDto}s
 * so the entity never leaves the service layer.
 */
@Component
public class GroupJoinRequestAssembler {

    public GroupJoinRequestDto toDto(GroupJoinRequest request) {
        return new GroupJoinRequestDto(
            request.getId(),
            request.getStudyGroup().getId(),
            request.getStudyGroup().getName(),
            request.getStudent().getId(),
            request.getStudent().getName(),
            request.getMessage(),
            request.getStatus(),
            request.getCreatedAt(), request.getRespondedAt(), request.getStudyGroup().isActive());
    }
}
