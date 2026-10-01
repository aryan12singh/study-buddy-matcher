package com.studybuddy.studygroup;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds entities with ids already set, as they would be after loading from
 * the database. Ids are assigned by JPA and have no setters, hence reflection.
 */
final class StudyGroupFixtures {

    private StudyGroupFixtures() {
    }

    static Student student(Long id, String name) {
        Student student = new Student(null, name, "SCIS", "Information Systems", 3, "+65 9000 000" + id);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    static Course course(Long id, String code) {
        Course course = new Course(code, code + " course");
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    static StudyGroup group(Long id, Course course, Student leader, int maxGroupSize) {
        StudyGroup group = new StudyGroup("Midterm crammers", "Weekly problem sets", course, leader, maxGroupSize);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    static GroupJoinRequest pendingJoinRequest(Long id, StudyGroup group, Student student) {
        GroupJoinRequest request = new GroupJoinRequest(group, student, "Can I join?");
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    static GroupMembership membership(StudyGroup group, Student student) {
        return new GroupMembership(group, student);
    }
}
