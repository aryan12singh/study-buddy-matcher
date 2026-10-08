package com.studybuddy.profile;

/** A course as it appears on a profile: enough to show it, no more. */
public record ProfileCourseDto(Long id, String code, String name) {
}
