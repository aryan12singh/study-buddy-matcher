package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** "Quiz me on polymorphism": hands over to Quiz Buddy, which writes quizzes from the student's notes. */
@Component
public class QuizMeIntent implements AssistantIntent {
    private final AssistantCourses courses;

    public QuizMeIntent(AssistantCourses courses) {
        this.courses = courses;
    }

    @Override
    public AssistantIntentType type() {
        return AssistantIntentType.QUIZ_ME;
    }

    @Override
    public AssistantReplyDto reply(Long actorId, AssistantRequest request) {
        Map<String, String> prefill = new LinkedHashMap<>();
        courses.byCode(request.courseCode()).map(Course::getId).ifPresent(id -> prefill.put("courseId", String.valueOf(id)));
        if (request.topic() != null) prefill.put("topic", request.topic());
        String text = "Quiz Buddy writes quizzes from your own notes. Open it and send your slides"
                + (request.topic() == null ? "." : ", and ask it to focus on “" + request.topic() + "”.");
        return AssistantReplyDto.text(type(), text,
                new AssistantActionDto(AssistantActionKind.OPEN_QUIZ_BUDDY, "Open Quiz Buddy", null, prefill));
    }
}
