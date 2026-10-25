package com.studybuddy.groupquiz;

import org.springframework.stereotype.Service;

/**
 * Starts a quiz in three steps: a short database read that checks the request, the AI call, and a
 * short database write. Deliberately not transactional: Gemini can take many seconds, and a
 * transaction held open that long would tie up a database connection.
 */
@Service
public class GroupQuizCreationService {
    private final GroupQuizService quizzes;
    private final QuizWriter writer;

    public GroupQuizCreationService(GroupQuizService quizzes, QuizWriter writer) {
        this.quizzes = quizzes;
        this.writer = writer;
    }

    public GroupQuizDto start(Long groupId, Long actorId, StartQuizInput input) {
        QuizStartRequest request = quizzes.prepareStart(groupId, actorId, input);
        QuizDraft draft = writer.write(actorId, request.course(), request.topics());
        return quizzes.saveStarted(actorId, request, draft);
    }
}
