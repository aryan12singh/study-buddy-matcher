package com.studybuddy.assistant;

import org.springframework.stereotype.Component;

/**
 * "Where do I change my availability?" or "take me to my groups": names the page and what it is for,
 * with a button to it. Only pages in {@link AppPage} can be suggested.
 */
@Component
public class NavigateIntent implements AssistantIntent {

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.NAVIGATE;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        AppPage page = request.page();
        if (page == null) {
            return AssistantReplyDto.text(type(), "I'm not sure which part of the app you mean. The Help page has a guide "
                    + "to each one, or tell me what you want to do, like “how do I join a study group?”",
                    AssistantActionDto.openPage("Open Help", AppPage.HELP.pathFor(actorId)));
        }
        String purpose = page.purpose();
        String text = "That's on " + page.title() + ": " + purpose + ".";
        return AssistantReplyDto.text(type(), text, AssistantActionDto.openPage("Go to " + page.title(), page.pathFor(actorId)));
    }
}
