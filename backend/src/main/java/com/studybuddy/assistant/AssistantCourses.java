package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/** Finds the course a student named by its code, however they spaced or cased it ("is 442"). */
@Component
public class AssistantCourses {
    private final CourseRepository courses;

    public AssistantCourses(CourseRepository courses) {
        this.courses = courses;
    }

    public Optional<Course> byCode(String typed) {
        if (typed == null) {
            return Optional.empty();
        }
        return courses.findByCode(typed.replaceAll("\\s+", "").toUpperCase(Locale.ROOT));
    }
}
