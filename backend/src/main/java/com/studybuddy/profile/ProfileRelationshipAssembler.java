package com.studybuddy.profile;

import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import org.springframework.stereotype.Component;

@Component
public class ProfileRelationshipAssembler {
    private final ConnectionRepository connections;
    private final MatchRequestRepository requests;

    public ProfileRelationshipAssembler(ConnectionRepository connections, MatchRequestRepository requests) {
        this.connections = connections;
        this.requests = requests;
    }

    public ProfileRelationshipDto assemble(Long subjectId, Long viewerId) {
        if (subjectId.equals(viewerId)) {
            return new ProfileRelationshipDto(RelationshipState.SELF, null, null);
        }
        var connection = connections.findActiveBetween(subjectId, viewerId);
        if (connection.isPresent()) {
            return new ProfileRelationshipDto(RelationshipState.CONNECTED, null, connection.get().getId());
        }
        var request = requests.findPendingBetween(subjectId, viewerId);
        if (request.isPresent()) return new ProfileRelationshipDto(request.get().isReceiver(viewerId)
            ? RelationshipState.INCOMING_PENDING : RelationshipState.OUTGOING_PENDING, request.get().getId(), null);
        return new ProfileRelationshipDto(RelationshipState.STRANGER, null, null);
    }
}
