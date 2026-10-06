package com.studybuddy.matchrequest;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.studygroup.CourseNotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchRequestService {
    private final MatchRequestRepository requests;
    private final ConnectionRepository connections;
    private final CourseRepository courses;
    private final MatchRequestAssembler assembler;
    private final NotificationService notifications;
    private final AccountAccess access;

    public MatchRequestService(MatchRequestRepository requests, ConnectionRepository connections, CourseRepository courses,
        MatchRequestAssembler assembler, NotificationService notifications, AccountAccess access) {
        this.requests = requests;
        this.connections = connections;
        this.courses = courses;
        this.assembler = assembler;
        this.notifications = notifications;
        this.access = access;
    }

    public MatchRequestDto send(Long senderId, Long receiverId, String message) {
        return send(senderId, receiverId, message, MatchRequestContext.profile());
    }

    public MatchRequestDto send(Long senderId, Long receiverId, String message, MatchRequestContext context) {
        InputRules.positiveId(receiverId, "Receiver");
        if (Objects.equals(senderId, receiverId)) {
            throw new MatchRequestNotAllowedException("You cannot send a match request to yourself");
        }
        access.lockStudents(senderId, senderId, receiverId);
        var sender = access.requireStudent(senderId);
        var receiver = access.eligibleStudent(receiverId);
        String normalMessage = InputRules.optional(message, "Message", InputRules.TEXT_LIMIT);
        context = context == null ? MatchRequestContext.profile() : context;
        if (context.origin() == null) {
            throw new InvalidInputException("context.origin", "Request origin is required");
        }
        Course course = null;
        if (context.courseId() != null) {
            InputRules.positiveId(context.courseId(), "Context course");
            Long courseId = context.courseId();
            course = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        }
        if (connections.existsActiveBetween(senderId, receiverId)) {
            throw new MatchRequestNotAllowedException("You are already connected with this student");
        }
        if (requests.existsPendingBetween(senderId, receiverId)) {
            throw new MatchRequestNotAllowedException("A match request between you and this student is already pending");
        }
        MatchRequest saved = requests.save(new MatchRequest(sender, receiver, normalMessage, context.origin(), course, context.studyGoal()));
        notifications.notify(receiver, NotificationType.MATCH_REQUEST_RECEIVED, sender.getName() + " sent you a study-buddy request",
            NotificationResourceType.MATCH_REQUEST, saved.getId(), "match:" + saved.getId() + ":received");
        return assembler.toDto(saved);
    }

    public MatchRequestDto accept(Long requestId, Long actorId) {
        MatchRequest request = forDecision(requestId, actorId);
        if (connections.existsActiveBetween(request.getSender().getId(), actorId)) {
            throw new MatchRequestNotAllowedException("You are already connected with this student");
        }
        request.accept();
        connections.save(new Connection(request.getSender(), request.getReceiver()));
        notifications.notify(request.getSender(), NotificationType.MATCH_REQUEST_ACCEPTED,
            request.getReceiver().getName() + " accepted your request. You can now see each other's contact number.",
            NotificationResourceType.MATCH_REQUEST, request.getId(), "match:" + request.getId() + ":accepted");
        return assembler.toDto(request);
    }

    public MatchRequestDto decline(Long requestId, Long actorId) {
        MatchRequest request = forDecision(requestId, actorId);
        request.decline();
        notifications.notify(request.getSender(), NotificationType.MATCH_REQUEST_DECLINED,
            request.getReceiver().getName() + " declined your study-buddy request", NotificationResourceType.MATCH_REQUEST,
            request.getId(), "match:" + request.getId() + ":declined");
        return assembler.toDto(request);
    }

    private MatchRequest forDecision(Long id, Long actorId) {
        access.beginWrite();
        access.requireStudent(actorId);
        var participants = requests.findParticipants(id).orElseThrow(() -> new MatchRequestNotFoundException(id));
        if (!Objects.equals(participants.getReceiverId(), actorId)) {
            throw new ForbiddenActionException("Only the student who received this request can respond");
        }
        access.lockStudents(actorId, participants.getSenderId(), participants.getReceiverId());
        return requests.findByIdForUpdate(id).orElseThrow(() -> new MatchRequestNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<MatchRequestDto> listIncoming(Long actorId) {
        access.requireStudent(actorId);
        return requests.findByReceiverIdOrderByCreatedAtDesc(actorId).stream().map(assembler::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchRequestDto> listOutgoing(Long actorId) {
        access.requireStudent(actorId);
        return requests.findBySenderIdOrderByCreatedAtDesc(actorId).stream().map(assembler::toDto).toList();
    }
}
