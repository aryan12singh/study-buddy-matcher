package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyMode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises the clearest messages with plain rules, so they are answered without an AI call: the
 * suggested questions, greetings, "what do I need to do?", "how do I change my availability?",
 * "find me an IS442 buddy who prefers small groups", "when can my group meet for 2 hours?", "plan our
 * IS442 group's next session", "what are we covering next session?". It never
 * guesses: a message with anything it cannot turn into a detail, such as "evenings" or a date, is
 * left for the AI.
 */
@Component
public class QuickIntentReader {
    private static final Pattern GREETING = Pattern.compile("^(hi|hello|hey|hiya|thanks|thank you|thx|ok|okay)\\b[\\s!.?]*(pixel)?[\\s!.?]*$");
    private static final Pattern NAVIGATE = Pattern.compile("^(take me to|go to|open|show me|where (is|are|do i|can i)|how (do|can) i)\\b");
    private static final Pattern TO_DO = Pattern.compile("\\b(what do i (need|have) to do|to-?dos?|to do list|pending|waiting (for|on) me|action items?"
            + "|anything (new|waiting|pending)|what'?s (new|next|waiting|pending)|catch me up)\\b");
    private static final Pattern QUIZ = Pattern.compile("\\b(quiz me|test me|practice questions|make (me )?a quiz)\\b");
    private static final Pattern QUIZ_TOPIC = Pattern.compile("\\b(?:on|about)\\s+(.+?)[?.!]*$", Pattern.CASE_INSENSITIVE);
    /**
     * Words shaped like a course code inside a quiz topic, with the "for" or "in" that leads to them.
     * "Java 101" has this shape too, so a match is taken out only when it is the catalogue course.
     */
    private static final Pattern TOPIC_COURSE = Pattern.compile("\\b(?:(?:for|in|from)\\s+)?([a-z]{2,4}\\s?\\d{3}[a-z]?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OWN_NOTES = Pattern.compile("(my|the|these|those) (notes|slides|lecture|lectures|readings)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIND = Pattern.compile("\\b(find|looking for|need|want|get)\\b.*\\b(buddy|buddies|partner|partners|study mate)\\b");
    private static final Pattern MEET = Pattern.compile("\\bwhen (can|could|should|do) (my|our|the|we)\\b.*\\bmeet\\b");
    private static final Pattern MEETING_GROUP = Pattern.compile("\\b(?:my|our|the)\\s+(.+?)\\s+group\\b");
    private static final Pattern HOURS = Pattern.compile("\\b(\\d+(?:\\.5)?)\\s*(?:hours?|hrs?|h)\\b");
    private static final Pattern MINUTES = Pattern.compile("\\b(\\d+)\\s*(?:minutes?|mins?)\\b");
    private static final Pattern PLAN_SESSION = Pattern.compile("\\b(plan|prepare|roadmap for)\\b.*\\b(sessions?|meetings?)\\b");
    private static final Pattern SESSION_TOPICS = Pattern.compile("\\bwhat (are|will|do|should) (we|i) (be )?(covering|cover|studying|study|going over)\\b"
            + "|\\bwhat'?s (on )?the (agenda|roadmap) for\\b|\\btopics? for (our|my|the|this|next)\\b.*\\b(sessions?|meetings?)\\b");
    private static final Pattern PLAN = Pattern.compile("\\b(study plan|revision plan|plan my (revision|studying|study|exam)|revision timetable)\\b");
    /** Details only the AI can read: times of day, goals, dates and the like. */
    private static final Pattern NEEDS_AI = Pattern.compile("\\b(morning|afternoon|evening|night|weekend|weekday|free|goal|exam|concept|problem|project"
            + "|discussion|hours?|minutes?|monday|tuesday|wednesday|thursday|friday|saturday|sunday|today|tomorrow|week|jan|feb|mar|apr|may|jun|jul|aug"
            + "|sep|sept|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)\\b|\\d");
    private static final Pattern COURSE_CODE = Pattern.compile("\\b([a-z]{2,4})\\s?(\\d{3}[a-z]?)\\b");
    private static final Pattern GROUP_SIZE_SMALL = Pattern.compile("\\bsmall groups?\\b");
    private static final Pattern GROUP_SIZE_ONE = Pattern.compile("\\b(one[- ]to[- ]one|one[- ]on[- ]one|1[- ]to[- ]1|1[- ]on[- ]1)\\b");
    /** Page words, most specific first, so "create a group" is not read as the group list. */
    private static final Map<Pattern, AppPage> PAGE_WORDS = new LinkedHashMap<>();
    static {
        PAGE_WORDS.put(Pattern.compile("\\b(availability|timetable|my courses|study goals?|edit (my )?profile|profile settings|preferences)\\b"), AppPage.EDIT_PROFILE);
        PAGE_WORDS.put(Pattern.compile("\\brequests?\\b"), AppPage.BUDDY_REQUESTS);
        PAGE_WORDS.put(Pattern.compile("\\b(create|start|make|new)\\s+(a\\s+)?(study\\s+)?group\\b"), AppPage.CREATE_GROUP);
        PAGE_WORDS.put(Pattern.compile("\\b(find|search for)\\s+(a\\s+)?(study\\s+)?(buddy|buddies|partner)\\b|\\bmatch(es|ing)?\\b"), AppPage.FIND_BUDDIES);
        PAGE_WORDS.put(Pattern.compile("\\bgroups?\\b"), AppPage.STUDY_GROUPS);
        PAGE_WORDS.put(Pattern.compile("\\b(sessions?|calendar)\\b"), AppPage.STUDY_SESSIONS);
        PAGE_WORDS.put(Pattern.compile("\\b(forest|trees?)\\b"), AppPage.FOREST);
        PAGE_WORDS.put(Pattern.compile("\\b(study room|timer|pomodoro|focus|music|quiz buddy)\\b"), AppPage.STUDY_ROOM);
        PAGE_WORDS.put(Pattern.compile("\\b(achievements?|badges?|streaks?)\\b"), AppPage.ACHIEVEMENTS);
        PAGE_WORDS.put(Pattern.compile("\\b(notifications?|alerts?)\\b"), AppPage.NOTIFICATIONS);
        PAGE_WORDS.put(Pattern.compile("\\b(connections?|my buddies|contacts?|contact numbers?)\\b"), AppPage.CONNECTIONS);
        PAGE_WORDS.put(Pattern.compile("\\bprofile\\b"), AppPage.MY_PROFILE);
        PAGE_WORDS.put(Pattern.compile("\\b(help|guides?)\\b"), AppPage.HELP);
        PAGE_WORDS.put(Pattern.compile("\\b(home|dashboard)\\b"), AppPage.HOME);
    }

    private final AssistantCourses courses;

    public QuickIntentReader(AssistantCourses courses) {
        this.courses = courses;
    }

    /** The request, when the message is clear enough to read without the AI. */
    public Optional<AssistantRequest> read(String message) {
        String text = message.strip().toLowerCase(Locale.ROOT);
        if (GREETING.matcher(text).matches()) {
            return Optional.of(AssistantRequest.unknown());
        }
        if (NAVIGATE.matcher(text).find()) {
            Optional<AppPage> page = pageIn(text);
            if (page.isPresent()) return Optional.of(request(AssistantIntentType.NAVIGATE).page(page.get()).build());
        }
        if (TO_DO.matcher(text).find()) {
            return Optional.of(request(AssistantIntentType.MY_TO_DO).build());
        }
        if (QUIZ.matcher(text).find()) {
            String course = catalogueCourseIn(text);
            return Optional.of(request(AssistantIntentType.QUIZ_ME).courseCode(course).topic(quizTopicIn(message.strip(), course)).build());
        }
        if (MEET.matcher(text).find()) {
            return Optional.of(request(AssistantIntentType.WHEN_TO_MEET).groupName(groupNameIn(text)).durationMinutes(minutesIn(text)).build());
        }
        boolean plain = !NEEDS_AI.matcher(text.replaceAll(COURSE_CODE.pattern(), "")).find();
        if (FIND.matcher(text).find() && plain && !text.contains("request")) {
            return Optional.of(request(AssistantIntentType.FIND_BUDDIES).courseCode(courseCodeIn(text))
                    .studyMode(studyModeIn(text)).groupSize(groupSizeIn(text)).build());
        }
        // A named day, such as "Thursday", is a date only the AI can work out.
        if (SESSION_TOPICS.matcher(text).find() && plain) {
            return Optional.of(request(AssistantIntentType.SESSION_TOPICS).courseCode(courseCodeIn(text)).groupName(groupNameIn(text)).build());
        }
        if (PLAN_SESSION.matcher(text).find() && plain) {
            return Optional.of(request(AssistantIntentType.PLAN_SESSION).courseCode(courseCodeIn(text)).groupName(groupNameIn(text)).build());
        }
        if (PLAN.matcher(text).find() && plain) {
            return Optional.of(request(AssistantIntentType.PLAN_STUDY).courseCode(courseCodeIn(text)).build());
        }
        return Optional.empty();
    }

    private static Optional<AppPage> pageIn(String text) {
        return PAGE_WORDS.entrySet().stream().filter(entry -> entry.getKey().matcher(text).find()).map(Map.Entry::getValue).findFirst();
    }

    private static String courseCodeIn(String text) {
        Matcher code = COURSE_CODE.matcher(text);
        return code.find() ? (code.group(1) + code.group(2)).toUpperCase(Locale.ROOT) : null;
    }

    /**
     * The first course code in the message that the catalogue holds, as the catalogue writes it.
     * A quiz topic such as "Java 101" or "SQL 201" has the shape of a code but names no course.
     */
    private String catalogueCourseIn(String text) {
        Matcher code = COURSE_CODE.matcher(text);
        while (code.find()) {
            Optional<Course> course = courses.byCode(code.group());
            if (course.isPresent()) return course.get().getCode();
        }
        return null;
    }

    /**
     * "Quiz me on Polymorphism" gives "Polymorphism", as the student typed it; "quiz me on my notes"
     * gives no topic. The course is carried separately, so "IS442 interfaces" gives "interfaces",
     * and only that course's code is taken out: "SQL 201 joins" stays whole.
     */
    private static String quizTopicIn(String message, String courseCode) {
        Matcher topic = QUIZ_TOPIC.matcher(message);
        if (!topic.find()) return null;
        String words = TOPIC_COURSE.matcher(topic.group(1))
                .replaceAll(found -> isCourse(found.group(1), courseCode) ? " " : Matcher.quoteReplacement(found.group()))
                .strip().replaceAll("\\s+", " ");
        return words.isEmpty() || OWN_NOTES.matcher(words).matches() ? null : words;
    }

    /** True when the typed words are the given course code, however they are spaced or cased. */
    private static boolean isCourse(String typed, String courseCode) {
        return typed.replaceAll("\\s+", "").equalsIgnoreCase(courseCode);
    }

    /** "my design patterns group" gives "design patterns"; "my group" gives no name. */
    private static String groupNameIn(String text) {
        Matcher group = MEETING_GROUP.matcher(text);
        return group.find() ? group.group(1).strip() : null;
    }

    private static Integer minutesIn(String text) {
        Matcher hours = HOURS.matcher(text);
        if (hours.find()) return (int) Math.round(Double.parseDouble(hours.group(1)) * 60);
        Matcher minutes = MINUTES.matcher(text);
        if (minutes.find()) return Integer.parseInt(minutes.group(1));
        if (text.contains("half an hour")) return 30;
        if (text.matches(".*\\b(an|one) hour\\b.*")) return 60;
        return null;
    }

    private static StudyMode studyModeIn(String text) {
        if (text.matches(".*\\bin[- ]person\\b.*")) return StudyMode.IN_PERSON;
        if (text.matches(".*\\bonline\\b.*")) return StudyMode.ONLINE;
        return null;
    }

    private static GroupSizePreference groupSizeIn(String text) {
        if (GROUP_SIZE_SMALL.matcher(text).find()) return GroupSizePreference.SMALL_GROUP;
        if (GROUP_SIZE_ONE.matcher(text).find()) return GroupSizePreference.ONE_TO_ONE;
        return null;
    }

    private static Builder request(AssistantIntentType intent) {
        return new Builder(intent);
    }

    /** Fills in only the details a rule found; everything else stays null, as with the AI. */
    private static final class Builder {
        private final AssistantIntentType intent;
        private String courseCode;
        private StudyMode studyMode;
        private GroupSizePreference groupSize;
        private String groupName;
        private Integer durationMinutes;
        private String topic;
        private AppPage page;

        private Builder(AssistantIntentType intent) { this.intent = intent; }
        Builder courseCode(String value) { courseCode = value; return this; }
        Builder studyMode(StudyMode value) { studyMode = value; return this; }
        Builder groupSize(GroupSizePreference value) { groupSize = value; return this; }
        Builder groupName(String value) { groupName = value; return this; }
        Builder durationMinutes(Integer value) { durationMinutes = value; return this; }
        Builder topic(String value) { topic = value; return this; }
        Builder page(AppPage value) { page = value; return this; }

        AssistantRequest build() {
            return new AssistantRequest(intent, courseCode, null, studyMode, groupSize, null, null, groupName,
                    durationMinutes, null, null, topic, page, null);
        }
    }
}
