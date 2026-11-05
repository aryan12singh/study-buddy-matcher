package com.studybuddy.seed;

import com.studybuddy.quiz.PersonalQuizInput;
import com.studybuddy.quiz.Quiz;
import com.studybuddy.quiz.QuizAnswer;
import com.studybuddy.quiz.QuizAttempt;
import com.studybuddy.quiz.QuizAttemptRepository;
import com.studybuddy.quiz.QuizCreationService;
import com.studybuddy.quiz.QuizDto;
import com.studybuddy.quiz.QuizQuestion;
import com.studybuddy.quiz.QuizRepository;
import com.studybuddy.quiz.QuizService;
import com.studybuddy.quiz.QuizSubmissionInput;
import com.studybuddy.quiz.StartQuizInput;
import com.studybuddy.seed.DemoData.QuizEntry;
import com.studybuddy.seed.DemoData.TakerEntry;
import com.studybuddy.student.StudentRepository;
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
 * questions pass the same checks as Gemini's reply. Each attempt is opened the taker's time before
 * the seed runs, so the leaderboard shows realistic times rather than everyone finishing at once.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoQuizSeeder {

    private final QuizCreationService creation;
    private final QuizService quizzes;
    private final QuizRepository quizRecords;
    private final QuizAttemptRepository attempts;
    private final StudentRepository students;
    private final JsonMapper json;
    private final Clock clock;

    public DemoQuizSeeder(QuizCreationService creation, QuizService quizzes, QuizRepository quizRecords,
            QuizAttemptRepository attempts, StudentRepository students, JsonMapper json, Clock clock) {
        this.creation = creation;
        this.quizzes = quizzes;
        this.quizRecords = quizRecords;
        this.attempts = attempts;
        this.students = students;
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
     * Opens the attempt {@code secondsTaken} ago, as the service would have when the taker asked for
     * the questions, then submits it now through the service, which marks and ranks it.
     */
    private void take(Long quizId, Long studentId, TakerEntry taker) {
        Quiz quiz = quizRecords.findById(quizId).orElseThrow();
        List<QuizQuestion> questions = quiz.getQuestions();
        if (taker.answers().size() != questions.size()) {
            throw new IllegalStateException("Demo quiz answers must cover every question");
        }
        attempts.save(QuizAttempt.start(quiz, students.findById(studentId).orElseThrow(),
                clock.instant().minusSeconds(taker.secondsTaken())));
        List<QuizAnswer> answers = IntStream.range(0, questions.size())
                .mapToObj(index -> new QuizAnswer(questions.get(index).getId(), taker.answers().get(index)))
                .toList();
        quizzes.submit(quizId, studentId, new QuizSubmissionInput(answers));
    }
}
