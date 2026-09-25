package com.studybuddy.matchrequest;

import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Sending and responding to match requests. The acting student's id is passed
 * in explicitly until authentication supplies it. Status changes on a loaded
 * request need no explicit save: the transaction flushes them on commit.
 */
@Service
@Transactional
public class MatchRequestService {

    private final MatchRequestRepository matchRequestRepository;
    private final ConnectionRepository connectionRepository;
    private final StudentRepository studentRepository;
    private final MatchRequestAssembler matchRequestAssembler;
    private final NotificationService notificationService;

    public MatchRequestService(MatchRequestRepository matchRequestRepository,
                               ConnectionRepository connectionRepository,
                               StudentRepository studentRepository,
                               MatchRequestAssembler matchRequestAssembler,
                               NotificationService notificationService) {
        this.matchRequestRepository = matchRequestRepository;
        this.connectionRepository = connectionRepository;
        this.studentRepository = studentRepository;
        this.matchRequestAssembler = matchRequestAssembler;
        this.notificationService = notificationService;
    }

    /**
     * @param message optional; blank is stored as no message
     * @throws MatchRequestNotAllowedException if sending to yourself, to a
     *         connected student, or while a request between the two is pending
     * @throws StudentNotFoundException if either student does not exist
     */
    public MatchRequestDto send(Long senderId, Long receiverId, String message) {
        if (Objects.equals(senderId, receiverId)) {
            throw new MatchRequestNotAllowedException("You cannot send a match request to yourself");
        }
        Student sender = findStudent(senderId);
        Student receiver = findStudent(receiverId);

        if (connectionRepository.existsActiveBetween(senderId, receiverId)) {
            throw new MatchRequestNotAllowedException("You are already connected with this student");
        }
        if (matchRequestRepository.existsPendingBetween(senderId, receiverId)) {
            throw new MatchRequestNotAllowedException(
                    "A match request between you and this student is already pending");
        }

        MatchRequest saved = matchRequestRepository.save(
                new MatchRequest(sender, receiver, normaliseMessage(message)));
        notificationService.notify(receiver, NotificationType.MATCH_REQUEST_RECEIVED,
                sender.getName() + " sent you a study-buddy request");
        return matchRequestAssembler.toDto(saved);
    }

    /**
     * Accepts the request and connects the two students.
     *
     * @throws MatchRequestNotFoundException if the request does not exist
     * @throws MatchRequestNotAllowedException if the student is not the receiver
     * @throws IllegalStateException if the request is no longer pending
     */
    public MatchRequestDto accept(Long requestId, Long currentStudentId) {
        MatchRequest request = findRequestForReceiver(requestId, currentStudentId);
        request.accept();
        connectionRepository.save(new Connection(request.getSender(), request.getReceiver()));
        notificationService.notify(request.getSender(), NotificationType.MATCH_REQUEST_ACCEPTED,
                request.getReceiver().getName()
                        + " accepted your request. You can now see each other's contact number.");
        return matchRequestAssembler.toDto(request);
    }

    /**
     * @throws MatchRequestNotFoundException if the request does not exist
     * @throws MatchRequestNotAllowedException if the student is not the receiver
     * @throws IllegalStateException if the request is no longer pending
     */
    public MatchRequestDto decline(Long requestId, Long currentStudentId) {
        MatchRequest request = findRequestForReceiver(requestId, currentStudentId);
        request.decline();
        notificationService.notify(request.getSender(), NotificationType.MATCH_REQUEST_DECLINED,
                request.getReceiver().getName() + " declined your study-buddy request");
        return matchRequestAssembler.toDto(request);
    }

    /** Requests sent to the student, newest first, in every status. */
    @Transactional(readOnly = true)
    public List<MatchRequestDto> listIncoming(Long studentId) {
        return toDtos(matchRequestRepository.findByReceiverIdOrderByCreatedAtDesc(studentId));
    }

    /** Requests sent by the student, newest first, in every status. */
    @Transactional(readOnly = true)
    public List<MatchRequestDto> listOutgoing(Long studentId) {
        return toDtos(matchRequestRepository.findBySenderIdOrderByCreatedAtDesc(studentId));
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private MatchRequest findRequestForReceiver(Long requestId, Long currentStudentId) {
        MatchRequest request = matchRequestRepository.findById(requestId)
                .orElseThrow(() -> new MatchRequestNotFoundException(requestId));
        if (!request.isReceiver(currentStudentId)) {
            throw new MatchRequestNotAllowedException(
                    "Only the student who received this match request can respond to it");
        }
        return request;
    }

    private List<MatchRequestDto> toDtos(List<MatchRequest> requests) {
        return requests.stream().map(matchRequestAssembler::toDto).toList();
    }

    private static String normaliseMessage(String message) {
        return message == null || message.isBlank() ? null : message.strip();
    }
}
