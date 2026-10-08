package com.studybuddy.admin;

import com.studybuddy.security.AccountPrincipal;
import com.studybuddy.user.Role;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminUserSummaryDto> list(@AuthenticationPrincipal AccountPrincipal actor, @RequestParam(required = false) Role role,
        @RequestParam(required = false) Boolean active, @RequestParam(required = false) String search) {
        return service.list(new AdminUserFilter(role, active, search), actor.id());
    }

    @GetMapping("/summary")
    public AdminAccountsSummaryDto summary(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.summary(actor.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserDetailDto create(@AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody AdminUserCreateRequest request) {
        return service.create(request, actor.id());
    }

    @GetMapping("/{id}")
    public AdminUserDetailDto get(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.get(id, actor.id());
    }

    @PutMapping("/{id}")
    public AdminUserDetailDto update(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody AdminUserUpdateRequest request) {
        return service.update(id, request, actor.id());
    }

    @PostMapping("/{id}/deactivate")
    public AdminUserDetailDto deactivate(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.deactivate(id, actor.id());
    }

    @PostMapping("/{id}/reactivate")
    public AdminUserDetailDto reactivate(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        return service.reactivate(id, actor.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal actor) {
        service.deletePermanently(id, actor.id());
    }
}
