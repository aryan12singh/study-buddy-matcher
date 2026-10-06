package com.studybuddy.auth;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public AuthResultDto login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResultDto register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @GetMapping("/me")
    public CurrentAccountDto me(@AuthenticationPrincipal AccountPrincipal actor) {
        return service.me(actor.id());
    }
}
