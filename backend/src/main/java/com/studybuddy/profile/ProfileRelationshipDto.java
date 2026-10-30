package com.studybuddy.profile;

/**
 * How the viewer relates to a profile's student, with the pending request or active connection id
 * the profile screen needs for its buttons.
 */
public record ProfileRelationshipDto(RelationshipState state, Long requestId, Long connectionId) {
}
