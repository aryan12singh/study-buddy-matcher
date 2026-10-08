package com.studybuddy.connection;

import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

/**
 * Converts {@link Connection} entities into {@link ConnectionDto}s, written
 * from the viewing student's side, so the entity never leaves the service
 * layer.
 */
@Component
public class ConnectionAssembler {

    public ConnectionDto toDto(Connection connection, Long viewerId) {
        Student other = connection.otherStudent(viewerId);
        return new ConnectionDto(
                connection.getId(),
                other.getId(),
                other.getName(),
                connection.getCreatedAt());
    }
}
