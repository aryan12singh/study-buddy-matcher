package com.studybuddy.groupquiz;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GroupQuizController {
    private final GroupQuizCreationService creation;
    private final GroupQuizService quizzes;

    public GroupQuizController(GroupQuizCreationService creation, GroupQuizService quizzes) {
        this.creation = creation;
        this.quizzes = quizzes;
    }

    @PostMapping("/groups/{groupId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupQuizDto start(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
            @Valid @RequestBody StartQuizInput input) {
        return creation.start(groupId, actor.id(), input);
    }

    @GetMapping("/groups/{groupId}/quizzes")
    public List<GroupQuizDto> list(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.list(groupId, actor.id());
    }

    @GetMapping("/quizzes/{quizId}")
    public GroupQuizDto get(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.get(quizId, actor.id());
    }

    @PostMapping("/quizzes/{quizId}/start")
    public QuizQuestionsDto startAttempt(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.startAttempt(quizId, actor.id());
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResultDto submit(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor,
            @Valid @RequestBody QuizSubmissionInput input) {
        return quizzes.submit(quizId, actor.id(), input);
    }

    @GetMapping("/quizzes/{quizId}/leaderboard")
    public LeaderboardDto leaderboard(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.leaderboard(quizId, actor.id());
    }

    @PostMapping("/quizzes/{quizId}/close")
    public GroupQuizDto close(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.close(quizId, actor.id());
    }

    @GetMapping("/quizzes/{quizId}/review")
    public QuizReviewDto review(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.review(quizId, actor.id());
    }
}
