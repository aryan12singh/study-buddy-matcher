package com.studybuddy.groupquiz;

import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studysession.StudySession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A quiz the group leader, room host or co-host started for a study group, with the questions
 * Gemini wrote. It stays open until {@code closesAt} or until it is closed early; an open quiz past
 * {@code closesAt} reads as closed, so no scheduler is needed. The same rules are enforced by the
 * table's check constraints.
 */
@Entity
@Table(name = "group_quizzes")
public class GroupQuiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "group_id", nullable = false) private StudyGroup studyGroup;
    @ManyToOne @JoinColumn(name = "session_id") private StudySession session;
    @Column(name = "created_by") private Long createdBy;
    @Column(nullable = false, length = 1000) private String topics;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private QuizStatus status;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<QuizQuestion> questions = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "closes_at", nullable = false) private Instant closesAt;
    /** Set only when the quiz was closed before it ran out of time. */
    @Column(name = "closed_at") private Instant closedAt;

    protected GroupQuiz() { }

    /**
     * Opens a quiz for the group, taking questions in the draft's order.
     *
     * @param session the session the quiz belongs to, or null for one started straight from the room
     */
    public static GroupQuiz open(StudyGroup group, StudySession session, Long actorId, String topics,
            QuizDraft draft, Instant now, Duration openFor) {
        if (draft.questions().isEmpty()) throw new IllegalArgumentException("A quiz needs at least one question");
        if (openFor.isZero() || openFor.isNegative()) throw new IllegalArgumentException("A quiz must stay open for some time");
        GroupQuiz quiz = new GroupQuiz();
        quiz.studyGroup = Objects.requireNonNull(group, "group");
        quiz.session = session;
        quiz.createdBy = actorId;
        quiz.topics = Objects.requireNonNull(topics, "topics");
        quiz.status = QuizStatus.OPEN;
        quiz.createdAt = now;
        quiz.closesAt = now.plus(openFor);
        for (QuizDraft.Question question : draft.questions()) {
            quiz.questions.add(new QuizQuestion(quiz, quiz.questions.size(), question.prompt(), question.options(),
                    question.correctOption(), question.explanation()));
        }
        return quiz;
    }

    /** The status members see now: an open quiz past its closing time counts as closed. */
    public QuizStatus statusAt(Instant now) {
        return status == QuizStatus.OPEN && now.isBefore(closesAt) ? QuizStatus.OPEN : QuizStatus.CLOSED;
    }

    public boolean isOpenAt(Instant now) {
        return statusAt(now) == QuizStatus.OPEN;
    }

    /**
     * Closes the quiz. Records the time only when it is closed early; closing a quiz that has already
     * run out of time just stores what members already see.
     */
    public void close(Instant now) {
        if (status == QuizStatus.CLOSED) return;
        if (now.isBefore(closesAt)) closedAt = now;
        status = QuizStatus.CLOSED;
    }

    /** When members stopped being able to submit: closed early, or the end of the open window. */
    public Instant endedAt() {
        return closedAt != null ? closedAt : closesAt;
    }

    public Long getId() { return id; }
    public StudyGroup getStudyGroup() { return studyGroup; }
    public StudySession getSession() { return session; }
    public Long getCreatedBy() { return createdBy; }
    public String getTopics() { return topics; }
    /** The stored status; use {@link #statusAt(Instant)} for what members see. */
    public QuizStatus getStatus() { return status; }
    public List<QuizQuestion> getQuestions() { return List.copyOf(questions); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosesAt() { return closesAt; }
    public Instant getClosedAt() { return closedAt; }
}
