package com.studybuddy.quiz;

import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
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
 * A multiple-choice quiz with the questions Gemini wrote: either for every member of a study group,
 * started by the leader or the room's host or co-host, or one student's own practice. It stays open
 * until {@code closesAt} or until it is closed early; an open quiz past {@code closesAt} reads as
 * closed, so no scheduler is needed. The same rules are enforced by the table's check constraints.
 */
@Entity
@Table(name = "quizzes")
public class Quiz {
    public static final int TITLE_LIMIT = 255;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private QuizScope scope;
    @ManyToOne @JoinColumn(name = "group_id") private StudyGroup studyGroup;
    /** The owner of a personal quiz. */
    @ManyToOne @JoinColumn(name = "student_id") private Student student;
    @ManyToOne @JoinColumn(name = "session_id") private StudySession session;
    @Column(name = "created_by") private Long createdBy;
    @ManyToOne(optional = false) @JoinColumn(name = "course_id", nullable = false) private Course course;
    /** What the quiz is on, shown in quiz lists: the focus given, or the uploaded files' names. */
    @Column(nullable = false, length = TITLE_LIMIT) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private QuizStatus status;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<QuizQuestion> questions = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "closes_at", nullable = false) private Instant closesAt;
    /** Set only when the quiz was closed before it ran out of time. */
    @Column(name = "closed_at") private Instant closedAt;

    protected Quiz() { }

    private Quiz(QuizScope scope, Course course, Long actorId, String title, QuizDraft draft, Instant now, Duration openFor) {
        if (draft.questions().isEmpty()) throw new IllegalArgumentException("A quiz needs at least one question");
        if (openFor.isZero() || openFor.isNegative()) throw new IllegalArgumentException("A quiz must stay open for some time");
        this.scope = scope;
        this.course = Objects.requireNonNull(course, "course");
        this.createdBy = actorId;
        this.title = Objects.requireNonNull(title, "title");
        this.status = QuizStatus.OPEN;
        this.createdAt = now;
        this.closesAt = now.plus(openFor);
        for (QuizDraft.Question question : draft.questions()) {
            questions.add(new QuizQuestion(this, questions.size(), question.prompt(), question.options(),
                    question.correctOption(), question.explanation()));
        }
    }

    /**
     * A quiz every member of the group may take once, on the group's course.
     *
     * @param session the group session the quiz belongs to, or null for one started from the room
     */
    public static Quiz forGroup(StudyGroup group, StudySession session, Long actorId, String title,
            QuizDraft draft, Instant now, Duration openFor) {
        Quiz quiz = new Quiz(QuizScope.GROUP, Objects.requireNonNull(group, "group").getCourse(), actorId, title,
                draft, now, openFor);
        quiz.studyGroup = group;
        quiz.session = session;
        return quiz;
    }

    /** A practice quiz only this student sees and takes. */
    public static Quiz personal(Student student, Course course, String title, QuizDraft draft, Instant now, Duration openFor) {
        Quiz quiz = new Quiz(QuizScope.PERSONAL, course, Objects.requireNonNull(student, "student").getId(), title,
                draft, now, openFor);
        quiz.student = student;
        return quiz;
    }

    /** The status takers see now: an open quiz past its closing time counts as closed. */
    public QuizStatus statusAt(Instant now) {
        return status == QuizStatus.OPEN && now.isBefore(closesAt) ? QuizStatus.OPEN : QuizStatus.CLOSED;
    }

    public boolean isOpenAt(Instant now) {
        return statusAt(now) == QuizStatus.OPEN;
    }

    /**
     * Closes the quiz. Records the time only when it is closed early; closing a quiz that has already
     * run out of time just stores what takers already see.
     */
    public void close(Instant now) {
        if (status == QuizStatus.CLOSED) return;
        if (now.isBefore(closesAt)) closedAt = now;
        status = QuizStatus.CLOSED;
    }

    /** A personal quiz has one taker, so it closes, revealing the answers, once they submit. */
    public void afterSubmission(Instant now) {
        if (scope == QuizScope.PERSONAL) close(now);
    }

    /** When takers stopped being able to submit: closed early, or the end of the open window. */
    public Instant endedAt() {
        return closedAt != null ? closedAt : closesAt;
    }

    public boolean isOwnedBy(Long studentId) {
        return scope == QuizScope.PERSONAL && Objects.equals(student.getId(), studentId);
    }

    public Long getId() { return id; }
    public QuizScope getScope() { return scope; }
    /** The group of a group quiz; null for a personal one. */
    public StudyGroup getStudyGroup() { return studyGroup; }
    /** The owner of a personal quiz; null for a group one. */
    public Student getStudent() { return student; }
    public StudySession getSession() { return session; }
    public Long getCreatedBy() { return createdBy; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    /** The stored status; use {@link #statusAt(Instant)} for what takers see. */
    public QuizStatus getStatus() { return status; }
    public List<QuizQuestion> getQuestions() { return List.copyOf(questions); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosesAt() { return closesAt; }
    public Instant getClosedAt() { return closedAt; }
}
