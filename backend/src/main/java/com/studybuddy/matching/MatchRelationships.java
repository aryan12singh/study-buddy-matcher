package com.studybuddy.matching;

import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequest;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.profile.RelationshipState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Where the searcher already stands with other students, so a match card can say
 * "Match requested" instead of offering a second request. Loads everything in
 * three queries rather than one per candidate. A connection outranks a pending
 * request, as on the profile page; anyone not listed is a stranger.
 */
@Component
public class MatchRelationships {

    private final ConnectionRepository connections;
    private final MatchRequestRepository requests;

    public MatchRelationships(ConnectionRepository connections, MatchRequestRepository requests) {
        this.connections = connections;
        this.requests = requests;
    }

    public Map<Long, RelationshipState> of(Long searcherId) {
        Map<Long, RelationshipState> states = new HashMap<>();
        for (MatchRequest sent : requests.findBySenderIdAndStatus(searcherId, MatchRequestStatus.PENDING)) {
            states.put(sent.getReceiver().getId(), RelationshipState.OUTGOING_PENDING);
        }
        for (MatchRequest received : requests.findByReceiverIdAndStatus(searcherId, MatchRequestStatus.PENDING)) {
            states.put(received.getSender().getId(), RelationshipState.INCOMING_PENDING);
        }
        for (Connection connection : connections.findActiveByStudentId(searcherId)) {
            states.put(otherStudentId(connection, searcherId), RelationshipState.CONNECTED);
        }
        return states;
    }

    private static Long otherStudentId(Connection connection, Long searcherId) {
        Long studentA = connection.getStudentA().getId();
        return studentA.equals(searcherId) ? connection.getStudentB().getId() : studentA;
    }
}
