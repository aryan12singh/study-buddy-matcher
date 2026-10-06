package com.studybuddy.admin;

/** Prefill fields only; contact is supplied as an optional replacement. */
public record AdminStudentProfileDto(
        String name,
        String school,
        String programme,
        Integer yearOfStudy) {
}
