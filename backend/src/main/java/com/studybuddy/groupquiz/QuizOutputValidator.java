package com.studybuddy.groupquiz;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Turns Gemini's quiz reply into a {@link QuizDraft}, or rejects it. A reply is accepted only if it
 * has exactly the configured number of questions, each with a prompt, four distinct options, a
 * correct option from 0 to 3 and an explanation, every text fitting its column. These are checks on
 * shape and size; they cannot prove the questions are right.
 */
@Component
public class QuizOutputValidator {
    private final QuizProperties properties;
    private final JsonMapper json;

    public QuizOutputValidator(QuizProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    public Optional<QuizDraft> validate(String raw) {
        JsonNode reply;
        try {
            reply = json.readTree(raw == null ? "" : raw);
        } catch (JacksonException notJson) {
            return Optional.empty();
        }
        JsonNode questions = reply.path("questions");
        if (!questions.isArray() || questions.size() != properties.questionCount()) {
            return Optional.empty();
        }
        List<QuizDraft.Question> parsed = new ArrayList<>();
        for (JsonNode question : questions) {
            Optional<QuizDraft.Question> one = questionOf(question);
            if (one.isEmpty()) {
                return Optional.empty();
            }
            parsed.add(one.get());
        }
        return Optional.of(new QuizDraft(parsed));
    }

    private static Optional<QuizDraft.Question> questionOf(JsonNode question) {
        Optional<String> prompt = textOf(question.path("prompt"), QuizQuestion.PROMPT_LIMIT);
        Optional<List<String>> options = optionsOf(question.path("options"));
        JsonNode correct = question.path("correctOption");
        Optional<String> explanation = textOf(question.path("explanation"), QuizQuestion.EXPLANATION_LIMIT);
        if (prompt.isEmpty() || options.isEmpty() || !isWholeNumber(correct)
                || !QuizQuestion.isOption(correct.asInt()) || explanation.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new QuizDraft.Question(prompt.get(), options.get(), correct.asInt(), explanation.get()));
    }

    /** Exactly four options, none blank or too long, and no two the same ignoring case. */
    private static Optional<List<String>> optionsOf(JsonNode node) {
        if (!node.isArray() || node.size() != QuizQuestion.OPTION_COUNT) {
            return Optional.empty();
        }
        List<String> options = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode item : node) {
            Optional<String> option = textOf(item, QuizQuestion.OPTION_LIMIT);
            if (option.isEmpty() || !seen.add(option.get().toLowerCase(Locale.ROOT))) {
                return Optional.empty();
            }
            options.add(option.get());
        }
        return Optional.of(options);
    }

    /** A non-blank string that fits its column once trimmed. */
    private static Optional<String> textOf(JsonNode node, int limit) {
        if (!node.isString()) {
            return Optional.empty();
        }
        String text = node.asString().strip();
        return text.isEmpty() || text.length() > limit ? Optional.empty() : Optional.of(text);
    }

    /** 2 or 2.0, but not "2", 2.5 or a number too large for an int. */
    private static boolean isWholeNumber(JsonNode node) {
        return node.isNumber() && node.canConvertToExactIntegral() && node.canConvertToInt();
    }
}
