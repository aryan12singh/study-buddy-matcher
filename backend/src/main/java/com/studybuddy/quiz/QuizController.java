package com.studybuddy.quiz;

import com.studybuddy.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final QuizCreationService creation;
    private final QuizService quizzes;

    public QuizController(QuizCreationService creation, QuizService quizzes) {
        this.creation = creation;
        this.quizzes = quizzes;
    }

    /** Multipart: the input's fields as form fields, plus zero or more parts named {@code files}. */
    @PostMapping(path = "/groups/{groupId}/quizzes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public QuizDto start(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor,
            @Valid @ModelAttribute StartQuizInput input, @RequestPart(name = "files", required = false) List<MultipartFile> files) {
        return creation.startForGroup(groupId, actor.id(), input, files);
    }

    /** Multipart: the input's fields as form fields, plus zero or more parts named {@code files}. */
    @PostMapping(path = "/quizzes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public QuizDto startPersonal(@AuthenticationPrincipal AccountPrincipal actor, @Valid @ModelAttribute PersonalQuizInput input,
            @RequestPart(name = "files", required = false) List<MultipartFile> files) {
        return creation.startPersonal(actor.id(), input, files);
    }

    @GetMapping("/quizzes")
    public List<QuizDto> mine(@AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.listMine(actor.id());
    }

    @GetMapping("/groups/{groupId}/quizzes")
    public List<QuizDto> list(@PathVariable Long groupId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.list(groupId, actor.id());
    }

    @GetMapping("/quizzes/{quizId}")
    public QuizDto get(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
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
    public QuizDto close(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.close(quizId, actor.id());
    }

    @GetMapping("/quizzes/{quizId}/review")
    public QuizReviewDto review(@PathVariable Long quizId, @AuthenticationPrincipal AccountPrincipal actor) {
        return quizzes.review(quizId, actor.id());
    }
}
