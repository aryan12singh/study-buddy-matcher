package com.studybuddy.course;

/** A course as the API shows it: id, code and name. */
public record CourseDto(Long id, String code, String name) {
}
