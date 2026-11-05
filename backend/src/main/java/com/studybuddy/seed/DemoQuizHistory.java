package com.studybuddy.seed;

import com.studybuddy.quiz.QuizAttempt;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dates a demo quiz attempt's start in the past. The quiz service opens an attempt at the moment
 * the taker asks for the questions, so attempts replayed together would all show a time of zero;
 * the demo leaderboard needs realistic times. Only the seed does this, on attempts it just opened.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoQuizHistory {

    private final JdbcTemplate database;
    private final EntityManager entities;

    public DemoQuizHistory(JdbcTemplate database, EntityManager entities) {
        this.database = database;
        this.entities = entities;
    }

    /** Moves the attempt's start to {@code startedAt}, then reloads it so the service submits from that time. */
    public void dateStart(Long quizId, Long studentId, Instant startedAt) {
        entities.flush();
        database.update("update quiz_attempts set started_at = ? where quiz_id = ? and student_id = ?",
                Timestamp.from(startedAt), quizId, studentId);
        entities.refresh(entities.createQuery(
                "select a from QuizAttempt a where a.quiz.id = :quizId and a.student.id = :studentId", QuizAttempt.class)
                .setParameter("quizId", quizId).setParameter("studentId", studentId).getSingleResult());
    }
}
