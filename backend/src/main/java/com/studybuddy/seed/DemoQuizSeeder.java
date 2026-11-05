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

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.studybuddy.seed.DemoSessionSeeder.groupIdOf;
import static com.studybuddy.seed.DemoSessionSeeder.sessionIdOf;
import static com.studybuddy.seed.DemoStudentSeeder.courseIdOf;

/**
 * Starts the demo's quizzes from questions written in advance, so seeding needs no Gemini key and
 * spends no allowance, then has each taker submit an attempt through the quiz service. The
 * questions pass the same checks as Gemini's reply. Each attempt is opened through the service and
 * then dated the taker's time back, so the leaderboard shows realistic times rather than everyone
 * finishing at once.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoQuizSeeder {

    private final QuizCreationService creation;
    private final QuizService quizzes;
    private final DemoQuizHistory history;
    private final JsonMapper json;
    private final Clock clock;

    public DemoQuizSeeder(QuizCreationService creation, QuizService quizzes, DemoQuizHistory history,
            JsonMapper json, Clock clock) {
        this.creation = creation;
        this.quizzes = quizzes;
        this.history = history;
        this.json = json;
        this.clock = clock;
    }

    public void seed(DemoData data, DemoRoster roster, Map<String, Long> courseIds, Map<String, Long> groupIds,
            Map<String, Long> sessionIds) {
        for (QuizEntry entry : data.quizzes()) {
            Long by = roster.id(entry.by());
            QuizDto quiz = start(entry, by, courseIds, groupIds, sessionIds);
            for (TakerEntry taker : entry.takers()) {
                take(quiz.id(), roster.id(taker.handle()), taker);
            }
            if (entry.close()) {
                quizzes.close(quiz.id(), by);
            }
        }
    }

    private QuizDto start(QuizEntry entry, Long by, Map<String, Long> courseIds, Map<String, Long> groupIds,
            Map<String, Long> sessionIds) {
        // Wrapped as Gemini's reply is, so the same validator reads it.
        String questions = json.writeValueAsString(Map.of("questions", entry.questions()));
        if (entry.group() != null) {
            Long sessionId = entry.session() == null ? null : sessionIdOf(entry.session(), sessionIds);
            return creation.startForGroupPrewritten(groupIdOf(entry.group(), groupIds), by,
                    new StartQuizInput(entry.notes(), entry.focus(), sessionId), questions);
        }
        return creation.startPersonalPrewritten(by,
                new PersonalQuizInput(courseIdOf(entry.course(), courseIds), entry.notes(), entry.focus()), questions);
    }

    /**
     * Opens the attempt through the quiz service, with its usual checks, dates the start
     * {@code secondsTaken} back, then submits it now through the service, which marks and ranks it.
     */
    private void take(Long quizId, Long studentId, TakerEntry taker) {
        List<QuizQuestionsDto.Question> questions = quizzes.startAttempt(quizId, studentId).questions();
        if (taker.answers().size() != questions.size()) {
            throw new IllegalStateException("Demo quiz answers must cover every question");
        }
        history.dateStart(quizId, studentId, clock.instant().minusSeconds(taker.secondsTaken()));
        List<QuizAnswer> answers = IntStream.range(0, questions.size())
                .mapToObj(index -> new QuizAnswer(questions.get(index).id(), taker.answers().get(index)))
                .toList();
        quizzes.submit(quizId, studentId, new QuizSubmissionInput(answers));
    }
}
