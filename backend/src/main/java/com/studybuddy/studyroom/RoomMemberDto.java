package com.studybuddy.studyroom;

/** A group member listed in the room, by id and name only. */
public record RoomMemberDto(Long studentId, String name) {}
