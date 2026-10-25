package com.studybuddy.groupquiz;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds quiz DTOs, so the entities never leave the service layer. Which DTO a member gets decides
 * what they can see: the questions DTO never carries the answers.
 */
@Component
public class GroupQuizAssembler {

    public GroupQuizDto toDto(GroupQuiz quiz, Optional<QuizAttempt> myAttempt, long submittedCount, boolean canHost, Instant now) {
        return new GroupQuizDto(quiz.getId(), quiz.getStudyGroup().getId(),
                quiz.getSession() == null ? null : quiz.getSession().getId(), quiz.getTopics(), quiz.statusAt(now),
                quiz.getCreatedBy(), quiz.getCreatedAt(), quiz.getClosesAt(), quiz.getClosedAt(),
                quiz.getQuestions().size(), submittedCount, stateOf(myAttempt), canHost);
    }

    public QuizQuestionsDto toQuestions(GroupQuiz quiz, QuizAttempt attempt) {
        List<QuizQuestionsDto.Question> questions = quiz.getQuestions().stream()
                .map(question -> new QuizQuestionsDto.Question(question.getId(), question.getPrompt(), question.getOptions()))
                .toList();
        return new QuizQuestionsDto(quiz.getId(), attempt.getStartedAt(), quiz.getClosesAt(), questions);
    }

    public AttemptResultDto toResult(QuizAttempt attempt, int rank) {
        return new AttemptResultDto(attempt.getScore(), attempt.getTotal(), attempt.timeTaken().toSeconds(), rank);
    }

    public LeaderboardDto toLeaderboard(GroupQuiz quiz, List<QuizLeaderboard.Ranked> ranked, long notSubmittedCount,
            Long actorId, Instant now) {
        List<LeaderboardDto.Entry> entries = ranked.stream().map(row -> {
            QuizResult result = row.result();
            return new LeaderboardDto.Entry(row.rank(), result.studentId(), result.displayName(), result.score(),
                    result.total(), result.timeTaken().toSeconds(), result.studentId().equals(actorId));
        }).toList();
        return new LeaderboardDto(quiz.getId(), quiz.statusAt(now), entries.size(), notSubmittedCount, entries);
    }

    public QuizReviewDto toReview(GroupQuiz quiz, Optional<QuizAttempt> myAttempt) {
        Optional<QuizAttempt> submitted = myAttempt.filter(QuizAttempt::isSubmitted);
        Map<Long, Integer> chosen = submitted.map(QuizAttempt::getAnswers).orElse(Map.of());
        List<QuizReviewDto.Question> questions = quiz.getQuestions().stream()
                .map(question -> new QuizReviewDto.Question(question.getId(), question.getPrompt(), question.getOptions(),
                        question.getCorrectOption(), chosen.get(question.getId()), question.getExplanation()))
                .toList();
        return new QuizReviewDto(quiz.getId(), submitted.map(QuizAttempt::getScore).orElse(null),
                submitted.map(QuizAttempt::getTotal).orElse(null), questions);
    }

    private static QuizAttemptState stateOf(Optional<QuizAttempt> attempt) {
        if (attempt.isEmpty()) return QuizAttemptState.NOT_STARTED;
        return attempt.get().isSubmitted() ? QuizAttemptState.SUBMITTED : QuizAttemptState.STARTED;
    }
}
