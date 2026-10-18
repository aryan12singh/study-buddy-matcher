package com.studybuddy.matchexplanation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Rejects malformed replies, success predictions, leftover code names and unsupported concrete
 * course/number references. These bounded checks are a guardrail, not a guarantee of factual
 * correctness. The same text policy applies to fresh provider replies and cached answers.
 */
public class ExplanationOutputValidator {
    /** Wording that presents a compatibility score as a prediction. */
    private static final Pattern SUCCESS_CLAIM = Pattern.compile("probabilit|guarantee|success|succeed|likel|chance|%",
            Pattern.CASE_INSENSITIVE);
    /**
     * A code name such as EXAM_PREPARATION or the quality label STRONG, which reads as a glitch.
     * Ordinary acronyms such as OOP are allowed.
     */
    private static final Pattern CODE_NAME = Pattern.compile("\\b[A-Z]+_[A-Z_]+\\b|\\b(?:STRONG|GOOD|FAIR)\\b");
    private static final Pattern COURSE_CODE = Pattern.compile("\\b[a-z]{2,}-?\\d{2,}[a-z0-9]*\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("(?<![\\w.])[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?![\\w.])");
    /** Number words, positioned so that each word's index is its value. */
    private static final List<String> NUMBER_WORDS = List.of("zero", "one", "two", "three", "four", "five", "six",
            "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen", "twenty");
    private static final Pattern NUMBER_WORD = Pattern.compile("\\b(?:" + String.join("|", NUMBER_WORDS) + ")\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_UNIT = Pattern.compile("^[\\s-]+(?:(?:shared|free)\\s+)?(?:hours?|hrs?)\\b",
            Pattern.CASE_INSENSITIVE);

    private final int maxCharacters;
    private final JsonMapper json;

    public ExplanationOutputValidator(int maxCharacters, JsonMapper json) {
        this.maxCharacters = maxCharacters;
        this.json = json;
    }

    public Optional<String> validate(String raw, ExplanationEvidence evidence) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        JsonNode reply;
        try {
            reply = json.readTree(raw);
        } catch (JacksonException notJson) {
            return Optional.empty();
        }
        JsonNode explanation = reply.path("explanation");
        return explanation.isString() ? validateText(explanation.asString(), evidence) : Optional.empty();
    }

    public Optional<String> validateText(String raw, ExplanationEvidence evidence) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.strip();
        if (text.isEmpty() || text.length() > maxCharacters || SUCCESS_CLAIM.matcher(text).find()
                || !hasOnlySupportedReferences(text, evidence)) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private static boolean hasOnlySupportedReferences(String text, ExplanationEvidence evidence) {
        // Remove the evidence's own course codes first, so a code such as COR-STAT1202 is
        // never mistaken for an unknown course or a code name.
        String rest = withoutEvidenceCourses(text, evidence);
        if (CODE_NAME.matcher(rest).find() || COURSE_CODE.matcher(rest).find()) {
            return false;
        }
        // Numbers, written as digits or words, may describe only the rounded weekly hours.
        return hoursMatch(rest, NUMBER, Double::parseDouble, true, evidence.sharedHoursPerWeek())
                && hoursMatch(rest, NUMBER_WORD, word -> NUMBER_WORDS.indexOf(word.toLowerCase(Locale.ROOT)), false,
                        evidence.sharedHoursPerWeek());
    }

    private static String withoutEvidenceCourses(String text, ExplanationEvidence evidence) {
        String rest = text;
        for (CourseEvidence course : Stream.of(evidence.scoringCourse(), evidence.sharedCourse())
                .filter(course -> course != null).toList()) {
            rest = Pattern.compile(Pattern.quote(course.code()), Pattern.CASE_INSENSITIVE).matcher(rest).replaceAll(" ");
        }
        return rest;
    }

    /**
     * Digits must always be a weekly-hours figure. Number words are common in ordinary phrases
     * ("one to one"), so they are checked only when they are followed by an hours unit.
     */
    private static boolean hoursMatch(String text, Pattern numbers, ToDoubleFunction<String> valueOf,
            boolean mustBeHours, double sharedHours) {
        Matcher number = numbers.matcher(text);
        while (number.find()) {
            boolean isHours = HOURS_UNIT.matcher(text.substring(number.end())).find();
            if (!isHours && mustBeHours) {
                return false;
            }
            if (isHours && valueOf.applyAsDouble(number.group()) != sharedHours) {
                return false;
            }
        }
        return true;
    }
}
