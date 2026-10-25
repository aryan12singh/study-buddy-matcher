package com.studybuddy.groupquiz;

import com.studybuddy.student.Student;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One member's single attempt at a quiz. Starts when the questions are sent to them; the answers,
 * score and submit time are recorded together when it is marked. The database allows one attempt
 * per member per quiz.
 */
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "quiz_id", nullable = false) private GroupQuiz quiz;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @Column(name = "started_at", nullable = false, updatable = false) private Instant startedAt;
    @Column(name = "submitted_at") private Instant submittedAt;
    private Integer score;
    private Integer total;
    /** The chosen option for each question, keyed by question ID. */
    @ElementCollection
    @CollectionTable(name = "quiz_attempt_answers", joinColumns = @JoinColumn(name = "attempt_id"))
    @MapKeyColumn(name = "question_id")
    @Column(name = "chosen_option", nullable = false)
    private Map<Long, Integer> answers = new HashMap<>();

    protected QuizAttempt() { }

    public static QuizAttempt start(GroupQuiz quiz, Student student, Instant now) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.quiz = Objects.requireNonNull(quiz, "quiz");
        attempt.student = Objects.requireNonNull(student, "student");
        attempt.startedAt = now;
        return attempt;
    }

    /** Records the marked answers. An attempt is submitted once and never changes afterwards. */
    public void submit(Map<Long, Integer> chosenOptions, int score, int total, Instant now) {
        if (isSubmitted()) throw new IllegalStateException("This attempt has already been submitted");
        if (total <= 0 || score < 0 || score > total) throw new IllegalArgumentException("The score must be between 0 and the total");
        if (now.isBefore(startedAt)) throw new IllegalArgumentException("An attempt cannot be submitted before it started");
        this.answers = new HashMap<>(chosenOptions);
        this.score = score;
        this.total = total;
        this.submittedAt = now;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    /** From receiving the questions to submitting; only for a submitted attempt. */
    public Duration timeTaken() {
        if (!isSubmitted()) throw new IllegalStateException("Only a submitted attempt has a time taken");
        return Duration.between(startedAt, submittedAt);
    }

    public Long getId() { return id; }
    public GroupQuiz getQuiz() { return quiz; }
    public Student getStudent() { return student; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Integer getScore() { return score; }
    public Integer getTotal() { return total; }
    public Map<Long, Integer> getAnswers() { return Map.copyOf(answers); }
}
