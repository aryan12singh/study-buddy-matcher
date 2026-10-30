package com.studybuddy.assistant;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@code POST /api/assistant/messages}, one turn of a chat with the Pixel study assistant.
 * It only delegates to {@link AssistantService}.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/messages")
    public AssistantReplyDto message(@AuthenticationPrincipal AccountPrincipal actor, @Valid @RequestBody AssistantMessageInput input) {
        return assistant.reply(actor.id(), input);
    }
}
