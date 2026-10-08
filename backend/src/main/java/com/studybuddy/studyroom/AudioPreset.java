package com.studybuddy.studyroom;

public enum AudioPreset {
    CALM_MUSIC("Calm melody", "MUSIC"),
    RAIN("Soft rain", "AMBIENT"),
    WHITE_NOISE("White noise", "AMBIENT"),
    CAFE("Cafe-style hum", "AMBIENT");

    private final String label;
    private final String kind;

    AudioPreset(String label, String kind) {
        this.label = label;
        this.kind = kind;
    }

    public String label() { return label; }
    public String kind() { return kind; }
}
