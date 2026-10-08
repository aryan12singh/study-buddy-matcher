package com.studybuddy.notification;

import com.studybuddy.matchrequest.MatchRequestRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NotificationAssembler {
    private final MatchRequestRepository requests;

    public NotificationAssembler(MatchRequestRepository requests) {
        this.requests = requests;
    }

    public NotificationDto toDto(Notification notification) {
        return toDto(notification, requestDirection(notification));
    }

    public List<NotificationDto> toDtos(List<Notification> notifications) {
        var requestIds = notifications.stream().filter(this::hasRequestResource)
            .map(Notification::getResourceId).collect(Collectors.toSet());
        Map<Long, MatchRequestRepository.RequestParticipants> participants = requestIds.isEmpty() ? Map.of()
            : requests.findParticipantsByIds(requestIds).stream().collect(Collectors.toMap(
                MatchRequestRepository.RequestParticipants::getId, Function.identity()));
        return notifications.stream().map(notification -> {
            var request = hasRequestResource(notification) ? participants.get(notification.getResourceId()) : null;
            return toDto(notification, request == null ? null : directionFor(notification.getRecipient().getId(), request));
        }).toList();
    }

    private NotificationDto toDto(Notification notification, NotificationRequestDirection direction) {
        return new NotificationDto(notification.getId(), notification.getType(), notification.getMessage(),
            notification.isRead(), notification.getCreatedAt(), notification.getResourceType(),
            notification.getResourceId(), notification.getEventKey(), direction);
    }

    private NotificationRequestDirection requestDirection(Notification notification) {
        if (!hasRequestResource(notification)) {
            return null;
        }
        return requests.findParticipants(notification.getResourceId())
            .map(participants -> directionFor(notification.getRecipient().getId(), participants))
            .orElse(null);
    }

    private boolean hasRequestResource(Notification notification) {
        return notification.getResourceType() == NotificationResourceType.MATCH_REQUEST && notification.getResourceId() != null;
    }

    private NotificationRequestDirection directionFor(Long recipientId, MatchRequestRepository.Participants participants) {
        if (recipientId.equals(participants.getReceiverId())) {
            return NotificationRequestDirection.INCOMING;
        }
        if (recipientId.equals(participants.getSenderId())) {
            return NotificationRequestDirection.OUTGOING;
        }
        return null;
    }
}
