package com.studybuddy.matchrequest;

import com.studybuddy.student.Student;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds entities with ids already set, as they would be after loading from
 * the database. Ids are assigned by JPA and have no setters, hence reflection.
 */
final class MatchRequestFixtures {

    private MatchRequestFixtures() {
    }

    static Student student(Long id, String name) {
        Student student = new Student(null, name, "SCIS", "Information Systems", 3, "+65 9000 000" + id);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    static MatchRequest pendingRequest(Long id, Student sender, Student receiver) {
        MatchRequest request = new MatchRequest(sender, receiver, "Want to study together?");
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
