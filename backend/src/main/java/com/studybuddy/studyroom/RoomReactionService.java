package com.studybuddy.studyroom;

import com.studybuddy.common.error.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends reactions from students who are in a room, and lists the ones still worth showing. */
@Service
@Transactional
public class RoomReactionService {
    private final StudyRoomRepository rooms;
    private final RoomPresenceRepository presences;
    private final RoomReactionRepository reactions;
    private final RoomAccess access;
    private final RoomReactionSettings settings;
    private final Clock clock;

    public RoomReactionService(StudyRoomRepository rooms, RoomPresenceRepository presences,
        RoomReactionRepository reactions, RoomAccess access, RoomReactionSettings settings,
        @Qualifier("studyRoomClock") Clock clock) {
        this.rooms = rooms;
        this.presences = presences;
        this.reactions = reactions;
        this.access = access;
        this.settings = settings;
        this.clock = clock;
    }

    /** Returns the saved reaction, so the sender can show it at once instead of waiting for a poll. */
    public RoomReactionDto send(Long groupId, Long actorId, RoomReactionType type) {
        access.readMember(groupId, actorId);
        Instant now = clock.instant();
        StudyRoom room = rooms.findByStudyGroupId(groupId).orElseThrow(this::notInRoom);
        var lease = presences.findEligible(room.getId(), now).stream()
            .filter(p -> p.getStudent().getId().equals(actorId)).findFirst().orElseThrow(this::notInRoom);
        reactions.deleteByStudyRoomIdAndSentAtBefore(room.getId(), now.minus(settings.retention()));
        if (reactions.countByStudyRoomIdAndStudentIdAndSentAtAfter(room.getId(), actorId,
            now.minus(settings.burstWindow())) >= settings.burstLimit()) {
            throw new ConflictException("You are reacting too quickly; wait a moment and try again");
        }
        RoomReaction saved = reactions.save(new RoomReaction(room, lease.getStudent(), type, now));
        return new RoomReactionDto(saved.getId(), type, actorId, lease.getStudent().getName(), now);
    }

    @Transactional(readOnly = true)
    public List<RoomReaction> recent(StudyRoom room, Instant now) {
        return room.getId() == null ? List.of()
            : reactions.findByStudyRoomIdAndSentAtAfterOrderByIdAsc(room.getId(), now.minus(settings.window()));
    }

    private ConflictException notInRoom() {
        return new ConflictException("Join the study room before sending a reaction");
    }
}
