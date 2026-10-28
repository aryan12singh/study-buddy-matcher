package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * "Plan my IS442 revision, exam on 20 Nov, 6 hours a week": hands the details to the Study plans
 * page, which asks for the notes and writes the roadmap. The link carries them as query parameters
 * ({@code courseId}, {@code examDate}, {@code hoursPerWeek}) so that page can fill in its form.
 */
@Component
public class PlanStudyIntent implements AssistantIntent {
    static final String STUDY_PLAN_PAGE = "/study-plan";
    private static final DateTimeFormatter EXAM_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);

    private final AssistantCourses courses;

    public PlanStudyIntent(AssistantCourses courses) {
        this.courses = courses;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.PLAN_STUDY;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        Optional<Course> course = courses.byCode(request.courseCode());
        Map<String, String> prefill = new LinkedHashMap<>();
        course.ifPresent(found -> prefill.put("courseId", String.valueOf(found.getId())));
        if (request.examDate() != null) prefill.put("examDate", request.examDate().toString());
        if (request.hoursPerWeek() != null) prefill.put("hoursPerWeek", String.valueOf(request.hoursPerWeek()));
        UriComponentsBuilder link = UriComponentsBuilder.fromPath(STUDY_PLAN_PAGE);
        prefill.forEach(link::queryParam);

        StringBuilder text = new StringBuilder("Let's plan your ")
                .append(course.map(found -> found.getCode() + " ").orElse("")).append("revision");
        if (request.examDate() != null) text.append(", with the exam on ").append(EXAM_DAY.format(request.examDate()));
        if (request.hoursPerWeek() != null) text.append(", ").append(AssistantWording.count(request.hoursPerWeek(), "hour", "hours")).append(" a week");
        text.append(". Open Study plans, attach your notes and I'll draft a week-by-week plan.");
        if (request.courseCode() != null && course.isEmpty()) {
            text.append(" I couldn't find a course called “").append(request.courseCode()).append("”, so pick it on the form.");
        }
        return AssistantReplyDto.text(type(), text.toString(),
                new AssistantActionDto(AssistantActionKind.OPEN_PAGE, "Open Study plans", link.build().toUriString(), prefill));
    }
}
