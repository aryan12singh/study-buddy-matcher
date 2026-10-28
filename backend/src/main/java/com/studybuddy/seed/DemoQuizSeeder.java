package com.studybuddy.seed;

import com.studybuddy.quiz.PersonalQuizInput;
import com.studybuddy.quiz.QuizAnswer;
import com.studybuddy.quiz.QuizCreationService;
import com.studybuddy.quiz.QuizDto;
import com.studybuddy.quiz.QuizQuestionsDto;
import com.studybuddy.quiz.QuizService;
import com.studybuddy.quiz.QuizSubmissionInput;
import com.studybuddy.quiz.StartQuizInput;
import com.studybuddy.seed.DemoData.QuizEntry;
import com.studybuddy.seed.DemoData.TakerEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.studybuddy.seed.DemoSessionSeeder.groupIdOf;
import static com.studybuddy.seed.DemoStudentSeeder.courseIdOf;

/**
 * Starts the demo's quizzes from questions written in advance, so seeding needs no Gemini key and
 * spends no allowance, then has each taker start and submit an attempt through the quiz service.
 * The questions pass the same checks as Gemini's reply.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoQuizSeeder {

    private final QuizCreationService creation;
    private final QuizService quizzes;
    private final JsonMapper json;

    public DemoQuizSeeder(QuizCreationService creation, QuizService quizzes, JsonMapper json) {
        this.creation = creation;
        this.quizzes = quizzes;
        this.json = json;
    }

    public void seed(DemoData data, DemoRoster roster, Map<String, Long> courseIds, Map<String, Long> groupIds) {
        for (QuizEntry entry : data.quizzes()) {
            Long by = roster.id(entry.by());
            QuizDto quiz = start(entry, by, courseIds, groupIds);
            for (TakerEntry taker : entry.takers()) {
                take(quiz.id(), roster.id(taker.handle()), taker.answers());
            }
            if (entry.close()) {
                quizzes.close(quiz.id(), by);
            }
        }
    }

    private QuizDto start(QuizEntry entry, Long by, Map<String, Long> courseIds, Map<String, Long> groupIds) {
        // Wrapped as Gemini's reply is, so the same validator reads it.
        String questions = json.writeValueAsString(Map.of("questions", entry.questions()));
        if (entry.group() != null) {
            return creation.startForGroupPrewritten(groupIdOf(entry.group(), groupIds), by,
                    new StartQuizInput(entry.notes(), entry.focus(), null), questions);
        }
        return creation.startPersonalPrewritten(by,
                new PersonalQuizInput(courseIdOf(entry.course(), courseIds), entry.notes(), entry.focus()), questions);
    }

    private void take(Long quizId, Long studentId, List<Integer> choices) {
        List<QuizQuestionsDto.Question> questions = quizzes.startAttempt(quizId, studentId).questions();
        if (choices.size() != questions.size()) {
            throw new IllegalStateException("Demo quiz answers must cover every question");
        }
        List<QuizAnswer> answers = IntStream.range(0, questions.size())
                .mapToObj(index -> new QuizAnswer(questions.get(index).id(), choices.get(index)))
                .toList();
        quizzes.submit(quizId, studentId, new QuizSubmissionInput(answers));
    }
}
