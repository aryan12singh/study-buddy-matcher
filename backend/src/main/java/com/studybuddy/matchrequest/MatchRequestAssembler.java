package com.studybuddy.matchrequest;

import org.springframework.stereotype.Component;

/**
 * Converts {@link MatchRequest} entities into {@link MatchRequestDto}s so the
 * entity never leaves the service layer.
 */
@Component
public class MatchRequestAssembler {

    public MatchRequestDto toDto(MatchRequest request) {
        return new MatchRequestDto(
                request.getId(),
                request.getSender().getId(),
                request.getSender().getName(),
                request.getReceiver().getId(),
                request.getReceiver().getName(),
                request.getMessage(),
                request.getStatus(),
                request.getCreatedAt());
    }
}
