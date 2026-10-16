package com.studybuddy.studyroom;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/room")
public class StudyRoomController {
    private final StudyRoomService service;

    public StudyRoomController(StudyRoomService service) { this.service = service; }

    @GetMapping
    public StudyRoomDto state(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.state(groupId, actor.id());
    }

    @PostMapping("/join")
    public StudyRoomDto join(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody RoomJoinInput input) { return service.join(groupId, actor.id(), input); }

    @PutMapping("/presence")
    public StudyRoomDto heartbeat(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody RoomHeartbeatInput input) { return service.heartbeat(groupId, actor.id(), input); }

    @DeleteMapping("/presence/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long groupId, @PathVariable UUID clientId,
        @AuthenticationPrincipal AccountPrincipal actor) { service.leave(groupId, actor.id(), clientId); }

    @PostMapping("/timer")
    public StudyRoomDto timer(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody RoomTimerInput input) { return service.controlTimer(groupId, actor.id(), input); }

    @PutMapping("/audio")
    public StudyRoomDto audio(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody RoomAudioInput input) { return service.audio(groupId, actor.id(), input); }

    @PutMapping("/settings")
    public StudyRoomDto configure(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody RoomConfigurationInput input) { return service.configure(groupId, actor.id(), input); }
}
