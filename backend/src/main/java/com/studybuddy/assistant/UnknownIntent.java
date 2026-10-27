package com.studybuddy.assistant;

import org.springframework.stereotype.Component;

/** Anything the assistant cannot do: says what it can, with an example. */
@Component
public class UnknownIntent implements AssistantIntent {
    static final String HELP = "I can tell you what's waiting for you, help you find your way around, find you a study buddy, "
            + "find a time your group can meet, quiz you on your notes, or plan your revision. "
            + "Try “what do I need to do?” or “how do I change my availability?”";

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.UNKNOWN;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        return AssistantReplyDto.text(type(), HELP, null);
    }
}
