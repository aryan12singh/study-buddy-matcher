package com.studybuddy.connection;

import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Listing and ending study-buddy connections, and answering whether two
 * students are connected. Connections are created by accepting a match
 * request, not here. The acting student's id is passed in explicitly until
 * authentication supplies it. Ending a loaded connection needs no explicit
 * save: the transaction flushes it on commit.
 */
@Service
@Transactional
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ConnectionAssembler connectionAssembler;
    private final NotificationService notificationService;

    public ConnectionService(ConnectionRepository connectionRepository,
                             ConnectionAssembler connectionAssembler,
                             NotificationService notificationService) {
        this.connectionRepository = connectionRepository;
        this.connectionAssembler = connectionAssembler;
        this.notificationService = notificationService;
    }

    /** The student's active connections, newest first. Ended ones are left out. */
    @Transactional(readOnly = true)
    public List<ConnectionDto> listActive(Long studentId) {
        return connectionRepository.findActiveByStudentId(studentId).stream()
                .map(connection -> connectionAssembler.toDto(connection, studentId))
                .toList();
    }

    /**
     * Ends the connection, so neither student sees the other's contact number
     * any more, and tells the other student.
     *
     * @throws ConnectionNotFoundException if the connection does not exist
     * @throws NotConnectionParticipantException if the student is not in it
     * @throws IllegalStateException if the connection has already ended
     */
    public void end(Long connectionId, Long currentStudentId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ConnectionNotFoundException(connectionId));
        if (!connection.involves(currentStudentId)) {
            throw new NotConnectionParticipantException(connectionId);
        }

        connection.end();
        notificationService.notify(connection.otherStudent(currentStudentId), NotificationType.CONNECTION_ENDED,
                participant(connection, currentStudentId).getName() + " ended your study-buddy connection");
    }

    /**
     * Ends every active connection the student has and tells each other
     * student. Used when the student's account is deactivated.
     */
    public void endAll(Long studentId) {
        for (Connection connection : connectionRepository.findActiveByStudentId(studentId)) {
            connection.end();
            notificationService.notify(connection.otherStudent(studentId), NotificationType.CONNECTION_ENDED,
                    "Your study-buddy connection with " + participant(connection, studentId).getName()
                            + " has ended");
        }
    }

    /** Whether the two students have an active connection, in either direction. */
    @Transactional(readOnly = true)
    public boolean areConnected(Long studentId, Long otherStudentId) {
        return connectionRepository.existsActiveBetween(studentId, otherStudentId);
    }

    /** The student's own side of a connection they are known to be part of. */
    private static Student participant(Connection connection, Long studentId) {
        return connection.getStudentA().getId().equals(studentId)
                ? connection.getStudentA()
                : connection.getStudentB();
    }
}
