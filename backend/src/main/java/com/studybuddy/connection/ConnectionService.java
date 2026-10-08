package com.studybuddy.connection;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConnectionService {
    private final ConnectionRepository connections;
    private final ConnectionAssembler assembler;
    private final NotificationService notifications;
    private final AccountAccess access;

    public ConnectionService(ConnectionRepository connections, ConnectionAssembler assembler, NotificationService notifications, AccountAccess access) {
        this.connections = connections;
        this.assembler = assembler;
        this.notifications = notifications;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> listActive(Long actorId) {
        access.requireStudent(actorId);
        return connections.findActiveByStudentId(actorId).stream().map(c -> assembler.toDto(c, actorId)).toList();
    }

    public void end(Long id, Long actorId) {
        access.beginWrite();
        access.requireStudent(actorId);
        var pair = connections.findParticipants(id).orElseThrow(() -> new ConnectionNotFoundException(id));
        if (!Objects.equals(pair.getStudentAId(), actorId) && !Objects.equals(pair.getStudentBId(), actorId)) {
            throw new NotConnectionParticipantException(id);
        }
        access.lockStudents(actorId, pair.getStudentAId(), pair.getStudentBId());
        Connection connection = connections.findByIdForUpdate(id).orElseThrow(() -> new ConnectionNotFoundException(id));
        connection.end();
        notifications.notify(connection.otherStudent(actorId), NotificationType.CONNECTION_ENDED,
            access.requireStudent(actorId).getName() + " ended your study-buddy connection", NotificationResourceType.STUDENT,
            actorId, "connection:" + id + ":ended:" + connection.otherStudent(actorId).getId());
    }
}
