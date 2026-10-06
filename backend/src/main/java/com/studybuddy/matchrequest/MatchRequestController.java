package com.studybuddy.matchrequest;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match-requests")
public class MatchRequestController {
    private final MatchRequestService service;

    public MatchRequestController(MatchRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchRequestDto send(@AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody SendMatchRequest request) {
        return service.send(actor.id(), request.receiverId(), request.message(), request.context());
    }

    @GetMapping("/incoming")
    public List<MatchRequestDto> incoming(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.listIncoming(actor.id());
    }

    @GetMapping("/outgoing")
    public List<MatchRequestDto> outgoing(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.listOutgoing(actor.id());
    }

    @PostMapping("/{id}/accept")
    public MatchRequestDto accept(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.accept(id, actor.id());
    }

    @PostMapping("/{id}/decline")
    public MatchRequestDto decline(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.decline(id, actor.id());
    }
}
