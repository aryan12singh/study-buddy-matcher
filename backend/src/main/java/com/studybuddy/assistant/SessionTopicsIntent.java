package com.studybuddy.assistant;

import com.studybuddy.studyplan.SessionPlanNotFoundException;
import com.studybuddy.studyplan.StudyPlanDto;
import com.studybuddy.studyplan.StudyPlanEntryDto;
import com.studybuddy.studyplan.StudyPlanService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * "What are we covering on Thursday?": finds the session meeting meant and lists that meeting's
 * topics from the session's roadmap. The topics are the roadmap's own entries; the AI only read
 * which day and group the student asked about.
 */
@Component
public class SessionTopicsIntent implements AssistantIntent {
    private final AssistantSessions sessions;
    private final StudyPlanService plans;

    public SessionTopicsIntent(AssistantSessions sessions, StudyPlanService plans) {
        this.sessions = sessions;
        this.plans = plans;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.SESSION_TOPICS;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        List<AssistantSessions.Upcoming> meant = sessions.meant(actorId, request);
        if (meant.isEmpty()) {
            String when = request.meetingDate() == null ? "" : " on " + AssistantSessions.DAY.format(request.meetingDate());
            return AssistantReplyDto.text(type(), "You have no upcoming study session" + when + " that I can find.",
                    AssistantActionDto.openPage("Open Study sessions", "/sessions"));
        }
        AssistantSessions.Upcoming session = meant.getFirst();
        AssistantActionDto open = AssistantActionDto.openPage("Open the session", session.link());
        Optional<StudyPlanDto> roadmap = roadmapOf(session, actorId);
        if (roadmap.isEmpty()) {
            String who = session.session().canManage() ? " You can plan it on the session page." : " The group leader can make one.";
            return AssistantReplyDto.text(type(), session.name() + " meets on " + session.day()
                    + " but has no roadmap yet." + who, open);
        }
        List<StudyPlanEntryDto> topics = roadmap.get().entries().stream()
                .filter(entry -> entry.scheduledDate().equals(session.meetingDay()))
                .toList();
        if (topics.isEmpty()) {
            return AssistantReplyDto.text(type(), "The roadmap for " + session.name() + " doesn't cover its meeting on "
                    + session.day() + ", so the session may have changed since it was made. Open the session to replace it.", open);
        }
        List<AssistantTodoDto> lines = topics.stream()
                .map(entry -> new AssistantTodoDto(line(entry), "Open the session", session.link()))
                .toList();
        String text = "On " + session.day() + ", " + session.name() + " covers "
                + AssistantWording.count(topics.size(), "topic", "topics") + ":";
        return new AssistantReplyDto(type(), text, List.of(), List.of(), List.of(), lines, open);
    }

    private Optional<StudyPlanDto> roadmapOf(AssistantSessions.Upcoming session, Long actorId) {
        try {
            return plans.getForSession(session.session().id(), actorId);
        } catch (SessionPlanNotFoundException none) {
            return Optional.empty();
        }
    }

    /** "Polymorphism, 45 minutes", ticked off when the leader has marked it done. */
    private static String line(StudyPlanEntryDto entry) {
        return (entry.completed() ? "✓ " : "") + entry.title() + ", " + AssistantWording.duration(entry.plannedMinutes());
    }
}
