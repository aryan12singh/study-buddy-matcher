package com.studybuddy.studyroom;

public record RoomAudioDto(AudioPreset musicPreset, boolean musicPlaying,
    AudioPreset ambientPreset, boolean ambientPlaying) {}
