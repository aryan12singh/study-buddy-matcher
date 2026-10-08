package com.studybuddy.notification;

import com.studybuddy.security.AccountPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal AccountPrincipal actor,
        @RequestParam(defaultValue = "ALL") NotificationFilter filter) {
        return service.list(actor.id(), filter);
    }

    @GetMapping("/unread-count")
    public UnreadCountDto unread(@AuthenticationPrincipal AccountPrincipal actor) {
        return new UnreadCountDto(service.unreadCount(actor.id()));
    }

    @PostMapping("/{id}/read")
    public NotificationDto read(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.markRead(id, actor.id());
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal AccountPrincipal actor) {
        service.markAllRead(actor.id());
    }

    public record UnreadCountDto(long count) {
    }
}
