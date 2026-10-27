package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchSearchResultDto;
import com.studybuddy.matching.MatchService;
import com.studybuddy.matching.MatchSort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "Find me an IS442 buddy who prefers small groups": runs the real match search, read-only, with the
 * filters the student stated, and shows the top matches. The search needs a course or a study goal,
 * so without either the assistant asks for one instead of guessing.
 */
@Component
public class FindBuddiesIntent implements AssistantIntent {
    private static final AssistantActionDto SEE_ALL = AssistantActionDto.openPage("See all on Find buddies", "/find-buddies");

    private final MatchService matches;
    private final AssistantCourses courses;
    private final AssistantProperties properties;

    public FindBuddiesIntent(MatchService matches, AssistantCourses courses, AssistantProperties properties) {
        this.matches = matches;
        this.courses = courses;
        this.properties = properties;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.FIND_BUDDIES;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        Optional<Course> course = courses.byCode(request.courseCode());
        if (request.courseCode() != null && course.isEmpty()) {
            return AssistantReplyDto.text(type(), "I couldn't find a course called “" + request.courseCode()
                    + "”. Try its code as it appears on your profile, like IS442.", SEE_ALL);
        }
        if (course.isEmpty() && request.studyGoal() == null) {
            return AssistantReplyDto.text(type(),
                    "Which course do you want a study buddy for? Try “find me an IS442 buddy”.", SEE_ALL);
        }
        // Time of day is not a search filter, so the closest honest ordering is by shared free time.
        boolean wantsTime = request.minSharedHours() != null || request.timeOfDay() != null;
        MatchSearchCriteria criteria = new MatchSearchCriteria(course.map(Course::getId).orElse(null),
                request.studyGoal(), request.studyMode(), request.groupSize(), request.minSharedHours(), null,
                wantsTime ? MatchSort.MOST_SHARED_HOURS : MatchSort.BEST_MATCH, properties.matchesShown());
        MatchSearchResultDto result = matches.search(criteria, actorId);
        return new AssistantReplyDto(type(), textFor(result, course, request), filtersOf(course, request),
                result.matches(), List.of(), List.of(), SEE_ALL);
    }

    private static String textFor(MatchSearchResultDto result, Optional<Course> course, AssistantRequest request) {
        String forCourse = course.map(found -> " for " + found.getCode()).orElse("");
        if (result.totalMatches() == 0) {
            return "I couldn't find anyone" + forCourse + " with those filters yet. Try fewer filters on Find buddies.";
        }
        String text = "I found " + AssistantWording.count(result.totalMatches(), "match", "matches") + forCourse + ". "
                + (result.matches().size() == 1 ? "Here they are." : "Here are the top " + result.matches().size() + ".");
        if (request.timeOfDay() != null) {
            text += " I can't filter by time of day yet, so they're sorted by the most free time you share.";
        }
        return text;
    }

    private static List<String> filtersOf(Optional<Course> course, AssistantRequest request) {
        List<String> filters = new ArrayList<>();
        course.ifPresent(found -> filters.add(found.getCode() + " " + found.getName()));
        if (request.studyGoal() != null) filters.add(AssistantWording.label(request.studyGoal()));
        if (request.studyMode() != null) filters.add(AssistantWording.label(request.studyMode()));
        if (request.groupSize() != null) filters.add(AssistantWording.label(request.groupSize()));
        if (request.minSharedHours() != null) {
            filters.add("At least " + AssistantWording.count(request.minSharedHours(), "shared hour", "shared hours") + " a week");
        }
        return filters;
    }
}
