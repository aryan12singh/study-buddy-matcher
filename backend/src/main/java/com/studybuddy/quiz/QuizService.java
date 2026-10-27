package com.studybuddy.quiz;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studysession.SessionStatus;
import com.studybuddy.studysession.StudySession;
import com.studybuddy.studysession.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists, takes, marks, ranks, closes and reviews group and personal quizzes, and does the two short
 * database steps of starting one. The AI call between those steps is made by
 * {@link QuizCreationService}, outside any transaction.
 */
@Service
@Transactional
public class QuizService {
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final GroupMembershipRepository memberships;
    private final StudySessionRepository sessions;
    private final CourseRepository courses;
    private final AccountAccess accounts;
    private final QuizAccess access;
    private final QuizMarker marker;
    private final QuizLeaderboard leaderboard;
    private final QuizAssembler assembler;
    private final QuizProperties properties;
    private final Clock clock;

    public QuizService(QuizRepository quizzes, QuizAttemptRepository attempts,
            GroupMembershipRepository memberships, StudySessionRepository sessions, CourseRepository courses,
            AccountAccess accounts,
            QuizAccess access, QuizMarker marker, QuizLeaderboard leaderboard, QuizAssembler assembler,
            QuizProperties properties, Clock clock) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.memberships = memberships;
        this.sessions = sessions;
        this.courses = courses;
        this.accounts = accounts;
        this.access = access;
        this.marker = marker;
        this.leaderboard = leaderboard;
        this.assembler = assembler;
        this.properties = properties;
        this.clock = clock;
    }

    /** Checks everything that needs no AI, so a bad request never spends the host's allowance. */
    @Transactional(readOnly = true)
    public QuizStartRequest prepareGroupStart(Long groupId, Long actorId, StartQuizInput input) {
        StudyGroup group = access.requireHost(groupId, actorId);
        String focus = checkedFocus(input.focus());
        if (input.sessionId() != null) {
            requireScheduledSessionOf(group, input.sessionId());
        }
        return new QuizStartRequest(group.getId(), group.getCourse(), focus, input.sessionId());
    }

    /** Checks everything that needs no AI, so a bad request never spends the student's allowance. */
    @Transactional(readOnly = true)
    public QuizStartRequest preparePersonalStart(Long actorId, PersonalQuizInput input) {
        accounts.requireStudent(actorId);
        Course course = courses.findById(input.courseId())
                .orElseThrow(() -> new InvalidInputException("courseId", "Choose one of the listed courses"));
        return new QuizStartRequest(null, course, checkedFocus(input.focus()), null);
    }

    /** Saves the validated questions, after checking again that the actor may still host. */
    public QuizDto saveGroupStarted(Long actorId, QuizStartRequest request, String title, QuizDraft draft) {
        accounts.beginWrite();
        StudyGroup group = access.requireHost(request.groupId(), actorId);
        // A session cancelled during the AI call leaves a quiz that belongs to no session.
        StudySession session = request.sessionId() == null ? null : sessions.findById(request.sessionId())
                .filter(found -> found.getStatus() == SessionStatus.SCHEDULED).orElse(null);
        Instant now = clock.instant();
        Quiz quiz = quizzes.save(Quiz.forGroup(group, session, actorId, title, draft, now,
                properties.openDuration()));
        return assembler.toDto(quiz, Optional.empty(), 0, true, now);
    }

    /** Saves the validated questions, after checking again that the student is still active. */
    public QuizDto savePersonalStarted(Long actorId, QuizStartRequest request, String title, QuizDraft draft) {
        accounts.beginWrite();
        Student student = accounts.requireStudent(actorId);
        Long courseId = request.course().getId();
        Course course = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        Instant now = clock.instant();
        Quiz quiz = quizzes.save(Quiz.personal(student, course, title, draft, now));
        return assembler.toDto(quiz, Optional.empty(), 0, true, now);
    }

    /** The student's own practice quizzes, newest first. */
    @Transactional(readOnly = true)
    public List<QuizDto> listMine(Long actorId) {
        accounts.requireStudent(actorId);
        Instant now = clock.instant();
        return quizzes.findByStudentIdOrderByCreatedAtDescIdDesc(actorId).stream()
                .map(quiz -> toDto(quiz, actorId, true, now))
                .toList();
    }

    /** The group's quizzes, newest first. */
    @Transactional(readOnly = true)
    public List<QuizDto> list(Long groupId, Long actorId) {
        StudyGroup group = access.requireMember(groupId, actorId);
        boolean canHost = access.canHost(group, actorId);
        Instant now = clock.instant();
        return quizzes.findByStudyGroupIdOrderByCreatedAtDescIdDesc(groupId).stream()
                .map(quiz -> toDto(quiz, actorId, canHost, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDto get(Long quizId, Long actorId) {
        Quiz quiz = readable(quizId, actorId);
        return toDto(quiz, actorId, access.canHost(quiz, actorId), clock.instant());
    }

    /**
     * Sends the questions and records when the member received them. Asking again returns the same
     * attempt with its original start time, so reloading the page does not reset the clock.
     */
    public QuizQuestionsDto startAttempt(Long quizId, Long actorId) {
        // Locking the member's own row makes a double click wait rather than start two attempts.
        accounts.lockStudents(actorId, actorId);
        Quiz quiz = readable(quizId, actorId);
        Optional<QuizAttempt> existing = attempts.findByQuizIdAndStudentId(quizId, actorId);
        if (existing.isPresent() && existing.get().isSubmitted()) {
            throw QuizStateConflictException.alreadySubmitted();
        }
        if (!quiz.isOpenAt(clock.instant())) {
            throw QuizStateConflictException.closed();
        }
        QuizAttempt attempt = existing.orElseGet(() -> {
            Student student = accounts.requireStudent(actorId);
            return attempts.save(QuizAttempt.start(quiz, student, clock.instant()));
        });
        return assembler.toQuestions(quiz, attempt);
    }

    public AttemptResultDto submit(Long quizId, Long actorId, QuizSubmissionInput input) {
        accounts.lockStudents(actorId, actorId);
        Quiz quiz = readable(quizId, actorId);
        QuizAttempt attempt = attempts.findByQuizIdAndStudentId(quizId, actorId)
                .orElseThrow(QuizStateConflictException::notStarted);
        if (attempt.isSubmitted()) {
            throw QuizStateConflictException.alreadySubmitted();
        }
        Instant now = clock.instant();
        if (!quiz.isOpenAt(now)) {
            throw QuizStateConflictException.closed();
        }
        QuizMark mark = marker.mark(quiz.getQuestions(), input.answers());
        attempt.submit(mark.chosenOptions(), mark.score(), mark.total(), now);
        quiz.afterSubmission(now);
        attempts.saveAndFlush(attempt);
        int rank = leaderboard.rankOf(currentResults(quiz), actorId).orElseThrow();
        return assembler.toResult(attempt, rank);
    }

    @Transactional(readOnly = true)
    public LeaderboardDto leaderboard(Long quizId, Long actorId) {
        Quiz quiz = readable(quizId, actorId);
        List<QuizResult> results = currentResults(quiz);
        long notSubmitted = takerIds(quiz).size() - results.size();
        return assembler.toLeaderboard(quiz, leaderboard.rank(results), notSubmitted, actorId, clock.instant());
    }

    public QuizDto close(Long quizId, Long actorId) {
        accounts.beginWrite();
        Quiz quiz = found(quizId);
        access.requireClosable(quiz, actorId);
        Instant now = clock.instant();
        quiz.close(now);
        return toDto(quiz, actorId, true, now);
    }

    /**
     * The answers and explanations, only once nobody can submit any more. A personal quiz closes when
     * its owner submits, so they see the answers straight away.
     */
    @Transactional(readOnly = true)
    public QuizReviewDto review(Long quizId, Long actorId) {
        Quiz quiz = readable(quizId, actorId);
        if (quiz.isOpenAt(clock.instant())) {
            throw QuizStateConflictException.notClosed();
        }
        return assembler.toReview(quiz, attempts.findByQuizIdAndStudentId(quizId, actorId));
    }

    private QuizDto toDto(Quiz quiz, Long actorId, boolean canHost, Instant now) {
        return assembler.toDto(quiz, attempts.findByQuizIdAndStudentId(quiz.getId(), actorId),
                attempts.countByQuizIdAndSubmittedAtIsNotNull(quiz.getId()), canHost, now);
    }

    /**
     * Submitted attempts by students who may still take the quiz. A member who left, was removed or
     * was deactivated keeps their attempt as history but drops off the leaderboard.
     */
    private List<QuizResult> currentResults(Quiz quiz) {
        Set<Long> takers = takerIds(quiz);
        return attempts.findByQuizIdAndSubmittedAtIsNotNull(quiz.getId()).stream()
                .filter(attempt -> takers.contains(attempt.getStudent().getId()))
                .map(QuizResult::of)
                .toList();
    }

    /** The group's current members, or a personal quiz's owner. */
    private Set<Long> takerIds(Quiz quiz) {
        if (quiz.getScope() == QuizScope.PERSONAL) {
            return Set.of(quiz.getStudent().getId());
        }
        return memberships.findByStudyGroupId(quiz.getStudyGroup().getId()).stream()
                .map(GroupMembership::getStudent)
                .map(Student::getId)
                .collect(Collectors.toSet());
    }

    /** The trimmed focus, or null when none was given. */
    private String checkedFocus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String focus = raw.strip();
        if (focus.length() > properties.maxFocusCharacters()) {
            throw new InvalidInputException("focus", "Keep the focus under " + properties.maxFocusCharacters() + " characters");
        }
        return focus;
    }

    private void requireScheduledSessionOf(StudyGroup group, Long sessionId) {
        boolean ours = sessions.findById(sessionId)
                .filter(session -> session.getGroup() != null && session.getGroup().getId().equals(group.getId()))
                .filter(session -> session.getStatus() == SessionStatus.SCHEDULED)
                .isPresent();
        if (!ours) {
            throw new InvalidInputException("sessionId", "Choose one of this group's scheduled sessions");
        }
    }

    private Quiz readable(Long quizId, Long actorId) {
        Quiz quiz = found(quizId);
        access.requireReadable(quiz, actorId);
        return quiz;
    }

    private Quiz found(Long quizId) {
        return quizzes.findById(quizId).orElseThrow(() -> new QuizNotFoundException(quizId));
    }
}
