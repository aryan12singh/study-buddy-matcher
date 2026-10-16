package com.studybuddy.weather;

/** Open-Meteo's published WMO code groups, used only for the decorative scene. */
public enum WeatherCondition {
    CLEAR, CLOUDY, RAIN, STORM, SNOW, FOG;

    public static WeatherCondition fromWmo(int code) {
        return switch (code) {
            case 0, 1 -> CLEAR;
            case 2, 3 -> CLOUDY;
            case 45, 48 -> FOG;
            case 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> RAIN;
            case 71, 73, 75, 77, 85, 86 -> SNOW;
            case 95, 96, 97, 99 -> STORM;
            default -> throw new IllegalArgumentException("Unsupported weather condition");
        };
    }
}
