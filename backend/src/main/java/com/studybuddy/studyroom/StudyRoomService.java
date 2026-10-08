package com.studybuddy.studyroom;

import com.studybuddy.common.error.ConflictException;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudyRoomService {
    private final StudyRoomRepository rooms;
    private final RoomPresenceRepository presences;
    private final GroupMembershipRepository memberships;
    private final RoomAccess access;
    private final RoomAssembler assembler;
    private final RoomSettings settings;
    private final Clock clock;

    public StudyRoomService(StudyRoomRepository rooms, RoomPresenceRepository presences,
        GroupMembershipRepository memberships, RoomAccess access, RoomAssembler assembler,
        RoomSettings settings, @Qualifier("studyRoomClock") Clock clock) {
        this.rooms = rooms;
        this.presences = presences;
        this.memberships = memberships;
        this.access = access;
        this.assembler = assembler;
        this.settings = settings;
        this.clock = clock;
    }

    public StudyRoomDto state(Long groupId, Long actorId) {
        StudyGroup group = access.lockMember(groupId, actorId);
        StudyRoom room = rooms.findByStudyGroupId(groupId).orElseGet(() -> new StudyRoom(group, settings));
        return detail(room, actorId);
    }

    public StudyRoomDto join(Long groupId, Long actorId, RoomJoinInput input) {
        StudyGroup group = access.lockMember(groupId, actorId);
        StudyRoom room = persistedRoom(group);
        Instant now = clock.instant();
        presences.deleteByStudyRoomIdAndExpiresAtLessThanEqual(room.getId(), now);
        presences.flush();
        var current = presences.findEligible(room.getId(), now);
        boolean alreadyHere = current.stream().anyMatch(p -> p.getStudent().getId().equals(actorId));
        if (!alreadyHere && participantCount(current) >= Math.min(room.getParticipantLimit(), group.getMaxGroupSize())) {
            throw new ConflictException("This study room is full");
        }
        var student = member(groupId, actorId);
        var lease = presences.findByStudyRoomIdAndStudentIdAndClientId(room.getId(), actorId, input.clientId())
            .orElseGet(() -> new RoomPresence(room, student, input.clientId(), now, now.plus(settings.leaseLifetime())));
        lease.renew(PresenceState.PRESENT, now, now.plus(settings.leaseLifetime()));
        presences.saveAndFlush(lease);
        return detail(room, actorId);
    }

    public StudyRoomDto heartbeat(Long groupId, Long actorId, RoomHeartbeatInput input) {
        StudyGroup group = access.lockMember(groupId, actorId);
        StudyRoom room = persistedRoom(group);
        Instant now = clock.instant();
        var lease = presences.findByStudyRoomIdAndStudentIdAndClientId(room.getId(), actorId, input.clientId())
            .filter(p -> p.getExpiresAt().isAfter(now))
            .orElseThrow(() -> new ConflictException("Your room presence expired; join the room again"));
        lease.renew(input.presence(), now, now.plus(settings.leaseLifetime()));
        presences.flush();
        return detail(room, actorId);
    }

    public void leave(Long groupId, Long actorId, UUID clientId) {
        access.lockMember(groupId, actorId);
        rooms.findByStudyGroupId(groupId).ifPresent(room ->
            presences.deleteByStudyRoomIdAndStudentIdAndClientId(room.getId(), actorId, clientId));
    }

    public StudyRoomDto controlTimer(Long groupId, Long actorId, RoomTimerInput input) {
        StudyRoom room = persistedRoom(access.lockMember(groupId, actorId));
        requireControl(room, actorId, input.expectedVersion());
        try {
            room.applyTimer(input.command().apply(room.timer(), clock.instant()));
        } catch (IllegalStateException invalid) {
            throw new ConflictException(invalid.getMessage());
        }
        rooms.flush();
        return detail(room, actorId);
    }

    public StudyRoomDto audio(Long groupId, Long actorId, RoomAudioInput input) {
        StudyRoom room = persistedRoom(access.lockMember(groupId, actorId));
        requireControl(room, actorId, input.expectedVersion());
        room.selectAudio(input.preset(), input.playing());
        rooms.flush();
        return detail(room, actorId);
    }

    public StudyRoomDto configure(Long groupId, Long actorId, RoomConfigurationInput input) {
        StudyGroup group = access.lockMember(groupId, actorId, input.hostId(), input.coHostId());
        if (!group.isLeader(actorId)) throw new ForbiddenActionException("Only the group leader can assign room roles and settings");
        StudyRoom room = persistedRoom(group);
        requireVersion(room, input.expectedVersion());
        if (input.focusMinutes() > settings.maxFocusMinutes()) {
            throw new InvalidInputException("focusMinutes", "Focus duration exceeds the configured limit");
        }
        if (input.breakMinutes() > settings.maxBreakMinutes()) {
            throw new InvalidInputException("breakMinutes", "Break duration exceeds the configured limit");
        }
        if (input.participantLimit() > group.getMaxGroupSize()
            || input.participantLimit() < participantCount(presences.findEligible(room.getId(), clock.instant()))) {
            throw new InvalidInputException("participantLimit", "Room capacity must fit the current participants and group capacity");
        }
        Student host = input.hostId() == null ? null : member(groupId, input.hostId());
        Student coHost = input.coHostId() == null ? null : member(groupId, input.coHostId());
        Long effectiveHost = host == null ? group.getLeader().getId() : host.getId();
        if (coHost != null && coHost.getId().equals(effectiveHost)) {
            throw new InvalidInputException("coHostId", "Host and co-host must be different members");
        }
        try {
            room.configure(input.focusMinutes(), input.breakMinutes(), input.participantLimit(), host, coHost);
        } catch (IllegalStateException invalid) {
            throw new ConflictException(invalid.getMessage());
        }
        rooms.flush();
        return detail(room, actorId);
    }

    private void requireControl(StudyRoom room, Long actorId, Long version) {
        if (!detail(room, actorId).canControl()) {
            throw new ForbiddenActionException("Only the leader or a present host/co-host can control the room");
        }
        requireVersion(room, version);
    }

    private void requireVersion(StudyRoom room, Long expectedVersion) {
        if (expectedVersion == null || room.getVersion() != expectedVersion) {
            throw new ConflictException("The room changed in another browser; refresh before trying again");
        }
    }

    private Student member(Long groupId, Long studentId) {
        return memberships.findByStudyGroupIdAndStudentId(groupId, studentId)
            .map(m -> m.getStudent()).orElseThrow(() -> new InvalidInputException("Only accepted members can hold room roles"));
    }

    private StudyRoom persistedRoom(StudyGroup group) {
        return rooms.findByStudyGroupId(group.getId()).orElseGet(() -> rooms.saveAndFlush(new StudyRoom(group, settings)));
    }

    private long participantCount(List<RoomPresence> current) {
        return current.stream().map(p -> p.getStudent().getId()).distinct().count();
    }

    private StudyRoomDto detail(StudyRoom room, Long actorId) {
        Instant now = clock.instant();
        return assembler.toDto(room, room.getId() == null ? List.of() : presences.findEligible(room.getId(), now),
            memberships.findByStudyGroupIdOrderByJoinedAtAsc(room.getStudyGroup().getId()), actorId, now);
    }
}
