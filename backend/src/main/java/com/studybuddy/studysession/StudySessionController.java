package com.studybuddy.studysession;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Study session endpoints: list the signed-in student's upcoming sessions, list, create and suggest
 * times for a group's or connection's sessions, and read, edit, cancel or download one as an
 * iCalendar file. Each handler delegates to {@link StudySessionService}.
 */
@RestController
@RequestMapping("/api")
public class StudySessionController {
    private final StudySessionService service;
    public StudySessionController(StudySessionService service) { this.service = service; }
    @GetMapping("/sessions/mine")
    public List<SessionDto> mine(@AuthenticationPrincipal AccountPrincipal actor) { return service.mine(actor.id()); }
    @GetMapping("/groups/{groupId}/sessions")
    public List<SessionDto> group(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor) { return service.groupSessions(groupId, actor.id()); }
    @GetMapping("/connections/{connectionId}/sessions")
    public List<SessionDto> buddy(@PathVariable Long connectionId, @AuthenticationPrincipal AccountPrincipal actor) { return service.buddySessions(connectionId, actor.id()); }
    @PostMapping("/groups/{groupId}/sessions") @ResponseStatus(HttpStatus.CREATED)
    public SessionDto createGroup(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody SessionInput input) { return service.createGroup(groupId, actor.id(), input); }
    @PostMapping("/connections/{connectionId}/sessions") @ResponseStatus(HttpStatus.CREATED)
    public SessionDto createBuddy(@PathVariable Long connectionId, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody SessionInput input) { return service.createBuddy(connectionId, actor.id(), input); }
    @GetMapping("/sessions/{id}")
    public SessionDto get(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) { return service.get(id, actor.id()); }
    @PutMapping("/sessions/{id}")
    public SessionDto edit(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody SessionInput input) { return service.edit(id, actor.id(), input); }
    @PostMapping("/sessions/{id}/cancel")
    public SessionDto cancel(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor,
        @Valid @RequestBody CancelSessionInput input) { return service.cancel(id, actor.id(), input); }
    @GetMapping("/groups/{groupId}/sessions/availability")
    public SessionAvailabilityDto groupAvailability(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor) { return service.groupAvailability(groupId, actor.id()); }
    @GetMapping("/connections/{connectionId}/sessions/availability")
    public SessionAvailabilityDto buddyAvailability(@PathVariable Long connectionId, @AuthenticationPrincipal AccountPrincipal actor) { return service.buddyAvailability(connectionId, actor.id()); }
    @GetMapping("/sessions/{id}/calendar")
    public ResponseEntity<String> calendar(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        CalendarExportDto export = service.calendar(id, actor.id());
        return ResponseEntity.ok().contentType(new MediaType("text", "calendar", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(export.filename()).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(export.content());
    }
}
