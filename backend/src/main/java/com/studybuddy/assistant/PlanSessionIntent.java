package com.studybuddy.assistant;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * "Plan our IS442 group's next session": finds the session meant and sends the student to its page,
 * where the session roadmap panel asks for the material and plans its upcoming meetings. Only whoever
 * manages the session can plan it, so anyone else is told who can.
 */
@Component
public class PlanSessionIntent implements AssistantIntent {
    private final AssistantSessions sessions;

    public PlanSessionIntent(AssistantSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.PLAN_SESSION;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        List<AssistantSessions.Upcoming> meant = sessions.meant(actorId, request);
        if (meant.isEmpty()) {
            return AssistantReplyDto.text(type(), noneText(request),
                    AssistantActionDto.openPage("Open Study sessions", "/sessions"));
        }
        AssistantSessions.Upcoming session = meant.getFirst();
        AssistantActionDto open = AssistantActionDto.openPage("Open the session", session.link());
        if (!session.session().canManage()) {
            return AssistantReplyDto.text(type(), "Only the group leader can plan " + session.name()
                    + ". Once they have, its roadmap shows on the session page.", open);
        }
        String text = "Let's plan " + session.name() + ", next meeting on " + session.day()
                + ". Open the session, attach your slides or paste the topics under Session roadmap,"
                + " and I'll plan what each upcoming meeting covers.";
        if (meant.size() > 1) text += " That's your soonest session; name its group to plan another.";
        return AssistantReplyDto.text(type(), text, open);
    }

    private static String noneText(AssistantRequest request) {
        if (request.groupName() == null && request.courseCode() == null && request.meetingDate() == null) {
            return "You don't have any upcoming study sessions to plan yet. Schedule one from a study group or a buddy connection first.";
        }
        return "I couldn't find an upcoming study session like that. Check its group's name, or open Study sessions to choose one.";
    }
}
