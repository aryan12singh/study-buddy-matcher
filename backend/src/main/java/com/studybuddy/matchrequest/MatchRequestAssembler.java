package com.studybuddy.matchrequest;

import org.springframework.stereotype.Component;

/**
 * Builds a {@link MatchRequestDto} from a {@link MatchRequest}, including where the request was
 * sent from. It names both students but never includes a contact number.
 */
@Component
public class MatchRequestAssembler {

    public MatchRequestDto toDto(MatchRequest request) {
        var course = request.getContextCourse();
        return new MatchRequestDto(request.getId(), request.getSender().getId(), request.getSender().getName(),
            request.getReceiver().getId(), request.getReceiver().getName(), request.getMessage(), request.getStatus(),
            request.getCreatedAt(), request.getRespondedAt(), new MatchRequestContextDto(request.getOrigin(),
            course == null ? null : course.getId(), course == null ? null : course.getCode(), course == null ? null : course.getName(),
            request.getContextStudyGoal()));
    }
}
