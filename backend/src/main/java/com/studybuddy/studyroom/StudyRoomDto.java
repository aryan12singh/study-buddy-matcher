package com.studybuddy.studyroom;

import java.time.Instant;
import java.util.List;

public record StudyRoomDto(Long groupId, String groupName, long version, Instant serverTime,
    int focusMinutes, int breakMinutes, int maxFocusMinutes, int maxBreakMinutes,
    int participantLimit, int groupLimit, Long hostId, Long coHostId, boolean hostOnline,
    boolean leader, boolean canControl, boolean joined, long pollIntervalMillis, long leaseLifetimeMillis,
    RoomTimerDto timer, RoomAudioDto audio, List<AudioPresetDto> audioPresets,
    List<RoomParticipantDto> participants, List<RoomMemberDto> members) {}
