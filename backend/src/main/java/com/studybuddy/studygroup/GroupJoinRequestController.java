package com.studybuddy.studygroup;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupJoinRequestController {
    private final GroupJoinRequestService service;

    public GroupJoinRequestController(GroupJoinRequestService service) {
        this.service = service;
    }

    @PostMapping("/api/groups/{id}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupJoinRequestDto request(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody JoinRequestBody body) {
        return service.request(id, actor.id(), body.message());
    }

    @GetMapping("/api/groups/{id}/join-requests")
    public List<GroupJoinRequestDto> list(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.listPending(id, actor.id());
    }

    @GetMapping("/api/group-join-requests/mine")
    public List<GroupJoinRequestDto> mine(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.mine(actor.id());
    }

    @PostMapping("/api/groups/{id}/join-requests/{requestId}/accept")
    public GroupJoinRequestDto accept(@PathVariable Long id, @PathVariable Long requestId, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.accept(id, requestId, actor.id());
    }

    @PostMapping("/api/groups/{id}/join-requests/{requestId}/reject")
    public GroupJoinRequestDto reject(@PathVariable Long id, @PathVariable Long requestId, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.reject(id, requestId, actor.id());
    }
    public record JoinRequestBody(@Size(max = 255) String message) {
    }
}
