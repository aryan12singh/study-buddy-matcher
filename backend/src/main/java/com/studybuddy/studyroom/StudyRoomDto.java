package com.studybuddy.studyroom;

import java.time.Instant;
import java.util.List;

/**
 * A group room as one member sees it: settings and limits, host and co-host, the shared timer, who
 * is present, the member list, recent reactions, and whether this viewer may control the room. It
 * carries names and ids only, never contact numbers.
 */
public record StudyRoomDto(Long groupId, String groupName, long version, Instant serverTime,
    int focusMinutes, int breakMinutes, int maxFocusMinutes, int maxBreakMinutes,
    int participantLimit, int groupLimit, Long hostId, Long coHostId, boolean hostOnline,
    boolean leader, boolean canControl, boolean joined, long pollIntervalMillis, long leaseLifetimeMillis,
    RoomTimerDto timer,
    List<RoomParticipantDto> participants, List<RoomMemberDto> members, List<RoomReactionDto> reactions) {}
