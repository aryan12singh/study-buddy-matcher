package com.studybuddy.notification;

import com.studybuddy.matchrequest.MatchRequestRepository;
import org.springframework.stereotype.Component;

@Component
public class NotificationAssembler {
    private final MatchRequestRepository requests;

    public NotificationAssembler(MatchRequestRepository requests) {
        this.requests = requests;
    }

    public NotificationDto toDto(Notification notification) {
        return new NotificationDto(notification.getId(), notification.getType(), notification.getMessage(),
            notification.isRead(), notification.getCreatedAt(), notification.getResourceType(),
            notification.getResourceId(), notification.getEventKey(), requestDirection(notification));
    }

    private NotificationRequestDirection requestDirection(Notification notification) {
        if (notification.getResourceType() != NotificationResourceType.MATCH_REQUEST
            || notification.getResourceId() == null) {
            return null;
        }
        return requests.findParticipants(notification.getResourceId())
            .map(participants -> directionFor(notification.getRecipient().getId(), participants))
            .orElse(null);
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
