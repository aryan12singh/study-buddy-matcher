package com.studybuddy.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Stores the course catalogue and finds a course by its code, as the demo seeder and the assistant
 * do.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);
}
