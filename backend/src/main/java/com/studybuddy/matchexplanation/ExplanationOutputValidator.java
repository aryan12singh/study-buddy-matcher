package com.studybuddy.matchexplanation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rejects malformed replies, obvious success predictions and unsupported concrete course/number
 * references. These bounded checks are a guardrail, not a guarantee of factual correctness.
 * The same text policy applies to fresh provider replies and cached answers.
 */
public class ExplanationOutputValidator {
    private static final Pattern SUCCESS_CLAIM = Pattern.compile("probabilit|guarantee|\\bsuccess\\b|\\bsucceed|%",
            Pattern.CASE_INSENSITIVE);
    /**
     * A code-style word such as EXAM_PREPARATION or STRONG, which reads as a glitch to a student.
     * Course codes contain digits, so IS442 does not match.
     */
    private static final Pattern CODE_STYLE = Pattern.compile("\\b[A-Z][A-Z_]{2,}\\b");
    private static final Pattern COURSE_CODE = Pattern.compile("\\b[a-z]{2,}-?\\d{2,}[a-z0-9]*\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("(?<![\\w.])[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?![\\w.])");
    private static final Pattern HOURS_UNIT = Pattern.compile("^\\s+(?:(?:shared|free)\\s+)?(?:hours?|hrs?)\\b", Pattern.CASE_INSENSITIVE);

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
                || CODE_STYLE.matcher(text).find()
                || !hasSupportedConcreteReferences(text, evidence)) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private static boolean hasSupportedConcreteReferences(String text, ExplanationEvidence evidence) {
        Set<String> courses = Stream.of(evidence.scoringCourse(), evidence.sharedCourse())
                .filter(course -> course != null).map(course -> course.code().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Matcher codes = COURSE_CODE.matcher(text);
        StringBuilder withoutCourses = new StringBuilder();
        while (codes.find()) {
            if (!courses.contains(codes.group().toUpperCase(Locale.ROOT))) {
                return false;
            }
            codes.appendReplacement(withoutCourses, " ");
        }
        codes.appendTail(withoutCourses);
        Matcher numbers = NUMBER.matcher(withoutCourses);
        while (numbers.find()) {
            // Numeric literals may describe only the rounded weekly hours supplied in evidence.
            if (!HOURS_UNIT.matcher(withoutCourses.substring(numbers.end())).find()
                    || Double.parseDouble(numbers.group()) != evidence.sharedHoursPerWeek()) {
                return false;
            }
        }
        return true;
    }
}
