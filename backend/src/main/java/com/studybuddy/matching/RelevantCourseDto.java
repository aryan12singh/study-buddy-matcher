package com.studybuddy.matching;

/** The searched course, or a deterministic shared course for a goal-only search. */
public record RelevantCourseDto(Long id, String code, String name) {
}
