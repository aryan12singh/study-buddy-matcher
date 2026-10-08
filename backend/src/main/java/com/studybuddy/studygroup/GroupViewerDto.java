package com.studybuddy.studygroup;

/** Caller-specific permissions and latest application history. */
public record GroupViewerDto(
        boolean leader,
        boolean member,
        Long requestId,
        GroupJoinRequestStatus requestStatus) {
}
