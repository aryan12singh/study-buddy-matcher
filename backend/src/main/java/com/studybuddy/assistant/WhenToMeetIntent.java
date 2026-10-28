package com.studybuddy.assistant;

import com.studybuddy.matching.SharedSlotDto;
import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.studygroup.StudyGroupSummaryDto;
import com.studybuddy.studysession.SessionAvailabilityDto;
import com.studybuddy.studysession.StudySessionService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * "When can my design patterns group meet for 2 hours?": finds which of the student's open groups
 * they mean, then lists the weekly times every member is free, from the existing session
 * availability, keeping only blocks long enough for the meeting.
 */
@Component
public class WhenToMeetIntent implements AssistantIntent {
    private final StudyGroupService groups;
    private final StudySessionService sessions;

    public WhenToMeetIntent(StudyGroupService groups, StudySessionService sessions) {
        this.groups = groups;
        this.sessions = sessions;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.WHEN_TO_MEET;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        List<StudyGroupSummaryDto> mine = groups.mine(actorId).stream().filter(StudyGroupSummaryDto::active).toList();
        if (mine.isEmpty()) {
            return AssistantReplyDto.text(type(), "You're not in any open study groups yet, so there's no group time to find.",
                    AssistantActionDto.openPage("Browse study groups", "/groups"));
        }
        Optional<StudyGroupSummaryDto> chosen = groupMeant(mine, request);
        if (chosen.isEmpty()) {
            String names = mine.stream().map(StudyGroupSummaryDto::name).collect(Collectors.joining(", "));
            return AssistantReplyDto.text(type(), "Which group do you mean? You're in " + names
                    + ". Try “when can my " + mine.get(0).name() + " group meet?”", null);
        }
        StudyGroupSummaryDto group = chosen.get();
        SessionAvailabilityDto availability = sessions.groupAvailability(group.id(), actorId);
        Integer minutes = request.durationMinutes();
        List<SharedSlotDto> slots = availability.sharedSlots().stream()
                .filter(slot -> minutes == null || Duration.between(slot.startTime(), slot.endTime()).toMinutes() >= minutes)
                .toList();
        AssistantActionDto schedule = AssistantActionDto.openPage("Schedule a session", "/groups/" + group.id() + "/sessions/new");
        String members = AssistantWording.count(availability.participantCount(), "member", "members");
        String forLength = minutes == null ? "" : " for " + AssistantWording.duration(minutes);
        if (slots.isEmpty()) {
            String text = availability.sharedSlots().isEmpty()
                    ? "There's no weekly time when all " + members + " of " + group.name() + " are free. Members may need to update their availability."
                    : group.name() + " has no weekly time when everyone is free" + forLength + ". Try a shorter session.";
            return AssistantReplyDto.text(type(), text, schedule);
        }
        String text = "All " + members + " of " + group.name() + " are free at "
                + (slots.size() == 1 ? "this time" : "these times") + " each week" + forLength
                + " (" + availability.timeZone() + " time):";
        return new AssistantReplyDto(type(), text, List.of(group.name()), List.of(), slots, List.of(), schedule);
    }

    /** The group named, by name or course code; or the only group when the student is in just one. */
    private static Optional<StudyGroupSummaryDto> groupMeant(List<StudyGroupSummaryDto> mine, AssistantRequest request) {
        String named = request.groupName() == null ? null : request.groupName().toLowerCase(Locale.ROOT);
        String code = request.courseCode() == null ? null : request.courseCode().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        List<StudyGroupSummaryDto> matching = mine.stream()
                .filter(group -> named != null && group.name().toLowerCase(Locale.ROOT).contains(named)
                        || code != null && code.equals(group.courseCode()))
                .toList();
        if (matching.size() == 1) return Optional.of(matching.get(0));
        if (named == null && code == null && mine.size() == 1) return Optional.of(mine.get(0));
        return Optional.empty();
    }
}
