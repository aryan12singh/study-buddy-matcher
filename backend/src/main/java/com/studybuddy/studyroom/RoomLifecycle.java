package com.studybuddy.studyroom;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Revokes ephemeral access within the transaction that ends membership or account access. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class RoomLifecycle {
    private final StudyRoomRepository rooms;
    private final RoomPresenceRepository presences;

    public RoomLifecycle(StudyRoomRepository rooms, RoomPresenceRepository presences) {
        this.rooms = rooms;
        this.presences = presences;
    }

    public void revokeMember(Long groupId, Long studentId) {
        presences.deleteByStudyRoomStudyGroupIdAndStudentId(groupId, studentId);
        rooms.findByStudyGroupId(groupId).ifPresent(room -> room.revokeRoles(studentId));
    }

    public void revokeStudent(Long studentId) {
        presences.deleteByStudentId(studentId);
        rooms.findByHostIdOrCoHostId(studentId, studentId).forEach(room -> room.revokeRoles(studentId));
    }

    public void close(Long groupId) {
        presences.deleteByStudyRoomStudyGroupId(groupId);
        rooms.findByStudyGroupId(groupId).ifPresent(StudyRoom::close);
    }
}
