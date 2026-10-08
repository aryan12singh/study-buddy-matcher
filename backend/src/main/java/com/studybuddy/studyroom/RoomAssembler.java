package com.studybuddy.studyroom;

import com.studybuddy.studygroup.GroupMembership;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoomAssembler {
    private final RoomSettings settings;

    public RoomAssembler(RoomSettings settings) { this.settings = settings; }

    public StudyRoomDto toDto(StudyRoom room, List<RoomPresence> leases, List<GroupMembership> memberships,
        Long actorId, Instant now) {
        var group = room.getStudyGroup();
        var members = memberships.stream().filter(m -> m.getStudent().getUser().isActive()).toList();
        var memberIds = members.stream().map(m -> m.getStudent().getId()).collect(Collectors.toSet());
        Long hostId = room.getHost() != null && memberIds.contains(room.getHost().getId())
            ? room.getHost().getId() : group.getLeader().getId();
        Long coHostId = room.getCoHost() != null && memberIds.contains(room.getCoHost().getId())
            && !room.getCoHost().getId().equals(hostId) ? room.getCoHost().getId() : null;
        var latest = leases.stream().collect(Collectors.toMap(p -> p.getStudent().getId(), p -> p,
            (first, later) -> first, LinkedHashMap::new));
        boolean joined = latest.containsKey(actorId);
        boolean leader = group.isLeader(actorId);
        var timer = room.timer().snapshot(now);
        return new StudyRoomDto(group.getId(), group.getName(), room.getVersion(), now,
            room.getFocusMinutes(), room.getBreakMinutes(), settings.maxFocusMinutes(), settings.maxBreakMinutes(),
            Math.min(room.getParticipantLimit(), group.getMaxGroupSize()), group.getMaxGroupSize(), hostId, coHostId,
            latest.containsKey(hostId), leader, leader || joined && (actorId.equals(hostId) || Objects.equals(actorId, coHostId)),
            joined, settings.pollInterval().toMillis(), settings.leaseLifetime().toMillis(),
            new RoomTimerDto(timer.phase(), timer.status(), timer.remainingAtAnchor().toMillis(),
                timer.focusDuration().toMillis(), timer.breakDuration().toMillis()),
            new RoomAudioDto(room.getAudioPreset(), room.isAudioPlaying()),
            Arrays.stream(AudioPreset.values()).map(p -> new AudioPresetDto(p, p.label(), p.kind())).toList(),
            latest.values().stream().map(p -> new RoomParticipantDto(p.getStudent().getId(), p.getStudent().getName(),
                p.getPresence(), p.getExpiresAt(), group.isLeader(p.getStudent().getId()),
                p.getStudent().getId().equals(hostId), Objects.equals(p.getStudent().getId(), coHostId))).toList(),
            members.stream().map(m -> new RoomMemberDto(m.getStudent().getId(), m.getStudent().getName())).toList());
    }
}
