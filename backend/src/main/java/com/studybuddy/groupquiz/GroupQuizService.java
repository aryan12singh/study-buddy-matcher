package com.studybuddy.groupquiz;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.InvalidInputException;
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
 * Lists, takes, marks, ranks, closes and reviews group quizzes, and does the two short database
 * steps of starting one. The AI call between those steps is made by {@link GroupQuizCreationService},
 * outside any transaction.
 */
@Service
@Transactional
public class GroupQuizService {
    private final GroupQuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final GroupMembershipRepository memberships;
    private final StudySessionRepository sessions;
    private final AccountAccess accounts;
    private final QuizAccess access;
    private final QuizMarker marker;
    private final QuizLeaderboard leaderboard;
    private final GroupQuizAssembler assembler;
    private final QuizProperties properties;
    private final Clock clock;

    public GroupQuizService(GroupQuizRepository quizzes, QuizAttemptRepository attempts,
            GroupMembershipRepository memberships, StudySessionRepository sessions, AccountAccess accounts,
            QuizAccess access, QuizMarker marker, QuizLeaderboard leaderboard, GroupQuizAssembler assembler,
            QuizProperties properties, Clock clock) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.memberships = memberships;
        this.sessions = sessions;
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
    public QuizStartRequest prepareStart(Long groupId, Long actorId, StartQuizInput input) {
        StudyGroup group = access.requireHost(groupId, actorId);
        String topics = input.topics().strip();
        if (topics.length() > properties.maxTopicsCharacters()) {
            throw new InvalidInputException("topics", "Keep the topics under " + properties.maxTopicsCharacters() + " characters");
        }
        if (input.sessionId() != null) {
            requireScheduledSessionOf(group, input.sessionId());
        }
        return new QuizStartRequest(group.getId(), group.getCourse(), topics, input.sessionId());
    }

    /** Saves the validated questions, after checking again that the actor may still host. */
    public GroupQuizDto saveStarted(Long actorId, QuizStartRequest request, QuizDraft draft) {
        accounts.beginWrite();
        StudyGroup group = access.requireHost(request.groupId(), actorId);
        // A session cancelled during the AI call leaves a quiz that belongs to no session.
        StudySession session = request.sessionId() == null ? null : sessions.findById(request.sessionId())
                .filter(found -> found.getStatus() == SessionStatus.SCHEDULED).orElse(null);
        Instant now = clock.instant();
        GroupQuiz quiz = quizzes.save(GroupQuiz.open(group, session, actorId, request.topics(), draft, now,
                properties.openDuration()));
        return assembler.toDto(quiz, Optional.empty(), 0, true, now);
    }

    /** The group's quizzes, newest first. */
    @Transactional(readOnly = true)
    public List<GroupQuizDto> list(Long groupId, Long actorId) {
        StudyGroup group = access.requireMember(groupId, actorId);
        boolean canHost = access.canHost(group, actorId);
        Instant now = clock.instant();
        return quizzes.findByStudyGroupIdOrderByCreatedAtDescIdDesc(groupId).stream()
                .map(quiz -> toDto(quiz, actorId, canHost, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupQuizDto get(Long quizId, Long actorId) {
        GroupQuiz quiz = readable(quizId, actorId);
        return toDto(quiz, actorId, access.canHost(quiz.getStudyGroup(), actorId), clock.instant());
    }

    /**
     * Sends the questions and records when the member received them. Asking again returns the same
     * attempt with its original start time, so reloading the page does not reset the clock.
     */
    public QuizQuestionsDto startAttempt(Long quizId, Long actorId) {
        // Locking the member's own row makes a double click wait rather than start two attempts.
        accounts.lockStudents(actorId, actorId);
        GroupQuiz quiz = readable(quizId, actorId);
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
        GroupQuiz quiz = readable(quizId, actorId);
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
        attempts.saveAndFlush(attempt);
        int rank = leaderboard.rankOf(currentResults(quiz), actorId).orElseThrow();
        return assembler.toResult(attempt, rank);
    }

    @Transactional(readOnly = true)
    public LeaderboardDto leaderboard(Long quizId, Long actorId) {
        GroupQuiz quiz = readable(quizId, actorId);
        List<QuizResult> results = currentResults(quiz);
        long notSubmitted = memberIds(quiz).size() - results.size();
        return assembler.toLeaderboard(quiz, leaderboard.rank(results), notSubmitted, actorId, clock.instant());
    }

    public GroupQuizDto close(Long quizId, Long actorId) {
        accounts.beginWrite();
        GroupQuiz quiz = found(quizId);
        access.requireClosable(quiz, actorId);
        Instant now = clock.instant();
        quiz.close(now);
        return toDto(quiz, actorId, true, now);
    }

    /** The answers and explanations, only once members can no longer submit. */
    @Transactional(readOnly = true)
    public QuizReviewDto review(Long quizId, Long actorId) {
        GroupQuiz quiz = readable(quizId, actorId);
        if (quiz.isOpenAt(clock.instant())) {
            throw QuizStateConflictException.notClosed();
        }
        return assembler.toReview(quiz, attempts.findByQuizIdAndStudentId(quizId, actorId));
    }

    private GroupQuizDto toDto(GroupQuiz quiz, Long actorId, boolean canHost, Instant now) {
        return assembler.toDto(quiz, attempts.findByQuizIdAndStudentId(quiz.getId(), actorId),
                attempts.countByQuizIdAndSubmittedAtIsNotNull(quiz.getId()), canHost, now);
    }

    /**
     * Submitted attempts by students who are still members. A member who left, was removed or was
     * deactivated keeps their attempt as history but drops off the leaderboard.
     */
    private List<QuizResult> currentResults(GroupQuiz quiz) {
        Set<Long> members = memberIds(quiz);
        return attempts.findByQuizIdAndSubmittedAtIsNotNull(quiz.getId()).stream()
                .filter(attempt -> members.contains(attempt.getStudent().getId()))
                .map(QuizResult::of)
                .toList();
    }

    private Set<Long> memberIds(GroupQuiz quiz) {
        return memberships.findByStudyGroupId(quiz.getStudyGroup().getId()).stream()
                .map(GroupMembership::getStudent)
                .map(Student::getId)
                .collect(Collectors.toSet());
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

    private GroupQuiz readable(Long quizId, Long actorId) {
        GroupQuiz quiz = found(quizId);
        access.requireReadable(quiz, actorId);
        return quiz;
    }

    private GroupQuiz found(Long quizId) {
        return quizzes.findById(quizId).orElseThrow(() -> new GroupQuizNotFoundException(quizId));
    }
}
