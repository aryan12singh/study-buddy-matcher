package com.studybuddy.studyroom;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes ephemeral access within the transaction that ends membership or account access. Each
 * student taken out of a room has their visit ended as if they had left, so a focus-forest visit
 * never stays open after its room access is gone.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class RoomLifecycle {
    private final StudyRoomRepository rooms;
    private final RoomPresenceRepository presences;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RoomLifecycle(StudyRoomRepository rooms, RoomPresenceRepository presences, ApplicationEventPublisher events,
        @Qualifier("studyRoomClock") Clock clock) {
        this.rooms = rooms;
        this.presences = presences;
        this.events = events;
        this.clock = clock;
    }

    public void revokeMember(Long groupId, Long studentId) {
        presences.deleteByStudyRoomStudyGroupIdAndStudentId(groupId, studentId);
        rooms.findByStudyGroupId(groupId).ifPresent(room -> room.revokeRoles(studentId));
        // Published even without a current tab: a visit can outlive its last lease.
        events.publishEvent(new RoomVisitEnded(studentId, groupId, clock.instant()));
    }

    public void revokeStudent(Long studentId) {
        presences.deleteByStudentId(studentId);
        rooms.findByHostIdOrCoHostId(studentId, studentId).forEach(room -> room.revokeRoles(studentId));
        // Covers every room, not just those with a current tab: a visit can outlive its last lease.
        events.publishEvent(new StudentRoomAccessRevoked(studentId, clock.instant()));
    }

    public void close(Long groupId) {
        presences.deleteByStudyRoomStudyGroupId(groupId);
        rooms.findByStudyGroupId(groupId).ifPresent(StudyRoom::close);
        events.publishEvent(new StudyRoomClosed(groupId, clock.instant()));
    }
}
