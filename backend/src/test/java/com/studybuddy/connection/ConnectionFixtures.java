package com.studybuddy.connection;

import com.studybuddy.student.Student;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds entities with ids already set, as they would be after loading from
 * the database. Ids are assigned by JPA and have no setters, hence reflection.
 */
final class ConnectionFixtures {

    private ConnectionFixtures() {
    }

    static Student student(Long id, String name) {
        Student student = new Student(null, name, "SCIS", "Information Systems", 3, "+65 9000 000" + id);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    static Connection connection(Long id, Student studentA, Student studentB) {
        Connection connection = new Connection(studentA, studentB);
        ReflectionTestUtils.setField(connection, "id", id);
        return connection;
    }
}
