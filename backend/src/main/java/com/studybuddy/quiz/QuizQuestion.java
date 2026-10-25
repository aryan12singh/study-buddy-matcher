package com.studybuddy.quiz;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * One multiple-choice question with exactly four options. Created only with its {@link Quiz}.
 * The correct option stays on the server until the quiz closes.
 */
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    public static final int OPTION_COUNT = 4;
    /** Column sizes, which Gemini's reply must also fit. */
    public static final int PROMPT_LIMIT = 500;
    public static final int OPTION_LIMIT = 255;
    public static final int EXPLANATION_LIMIT = 1000;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "quiz_id", nullable = false) private Quiz quiz;
    @Column(nullable = false) private int position;
    @Column(nullable = false, length = PROMPT_LIMIT) private String prompt;
    @ElementCollection
    @CollectionTable(name = "quiz_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    @Column(name = "text", nullable = false, length = OPTION_LIMIT)
    private List<String> options = new ArrayList<>();
    @Column(name = "correct_option", nullable = false) private int correctOption;
    @Column(nullable = false, length = EXPLANATION_LIMIT) private String explanation;

    protected QuizQuestion() { }

    QuizQuestion(Quiz quiz, int position, String prompt, List<String> options, int correctOption, String explanation) {
        if (options.size() != OPTION_COUNT) throw new IllegalArgumentException("A question needs exactly four options");
        if (!isOption(correctOption)) throw new IllegalArgumentException("The correct option must be one of the four");
        this.quiz = quiz;
        this.position = position;
        this.prompt = prompt;
        this.options = new ArrayList<>(options);
        this.correctOption = correctOption;
        this.explanation = explanation;
    }

    public static boolean isOption(int index) {
        return index >= 0 && index < OPTION_COUNT;
    }

    public boolean isCorrect(int chosenOption) {
        return chosenOption == correctOption;
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public int getPosition() { return position; }
    public String getPrompt() { return prompt; }
    public List<String> getOptions() { return List.copyOf(options); }
    public int getCorrectOption() { return correctOption; }
    public String getExplanation() { return explanation; }
}
