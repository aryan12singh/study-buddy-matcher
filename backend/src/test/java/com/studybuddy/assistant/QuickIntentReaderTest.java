package com.studybuddy.assistant;

import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuickIntentReaderTest {
    private static final QuickIntentReader READER = new QuickIntentReader(catalogueWithIs442());

    /** A catalogue holding IS442 only, so every other code-shaped word is not a course. */
    private static AssistantCourses catalogueWithIs442() {
        CourseRepository catalogue = mock(CourseRepository.class);
        when(catalogue.findByCode("IS442")).thenReturn(Optional.of(new Course("IS442", "Object Oriented Programming")));
        return new AssistantCourses(catalogue);
    }

    private static AssistantRequest read(String message) {
        return READER.read(message).orElseThrow(() -> new AssertionError("Expected a quick reading of: " + message));
    }

    @Test
    void readsEverySuggestedQuestionWithoutTheAi() {
        assertEquals(AssistantIntentType.MY_TO_DO, read("What do I need to do?").intent());
        assertEquals(AssistantIntentType.NAVIGATE, read("How do I change my availability?").intent());
        assertEquals(AssistantIntentType.QUIZ_ME, read("Quiz me on my notes").intent());
        assertEquals(AssistantIntentType.WHEN_TO_MEET, read("When can my group meet for 2 hours?").intent());
        assertEquals(AssistantIntentType.FIND_BUDDIES, read("Find me an IS442 buddy who prefers small groups").intent());
    }

    @Test
    void readsPlainQuestionsAboutStudySessionsWithoutTheAi() {
        AssistantRequest plan = read("Plan our IS442 group's next session");
        AssistantRequest topics = read("What are we covering next session?");
        AssistantRequest named = read("what are we covering in our design patterns group?");

        assertEquals(AssistantIntentType.PLAN_SESSION, plan.intent());
        assertEquals("IS442", plan.courseCode());
        assertEquals(AssistantIntentType.SESSION_TOPICS, topics.intent());
        assertNull(topics.meetingDate());
        assertEquals(AssistantIntentType.SESSION_TOPICS, named.intent());
        assertEquals("design patterns", named.groupName());
        assertEquals(AssistantIntentType.PLAN_STUDY, read("Plan my revision for IS442").intent(), "Personal revision is not a session");
    }

    @ParameterizedTest
    @ValueSource(strings = {"What are we covering on Thursday?", "Plan our session tomorrow"})
    void leavesASessionQuestionWithADayForTheAi(String message) {
        assertTrue(READER.read(message).isEmpty(), message);
    }

    @Test
    void readsTheDetailsOfABuddySearch() {
        AssistantRequest request = read("find me an is 442 study partner, in person, one-to-one");

        assertEquals("IS442", request.courseCode());
        assertEquals(StudyMode.IN_PERSON, request.studyMode());
        assertEquals(GroupSizePreference.ONE_TO_ONE, request.groupSize());
    }

    @Test
    void readsTheGroupAndLengthOfAMeeting() {
        AssistantRequest twoHours = read("When can my design patterns group meet for 2 hours?");
        AssistantRequest halfHour = read("when can our group meet for half an hour");

        assertEquals("design patterns", twoHours.groupName());
        assertEquals(120, twoHours.durationMinutes());
        assertNull(halfHour.groupName());
        assertEquals(30, halfHour.durationMinutes());
        assertEquals(90, read("when can we meet for 1.5 hours").durationMinutes());
    }

    @Test
    void sendsNavigationToTheMostSpecificPage() {
        assertEquals(AppPage.EDIT_PROFILE, read("how do I change my availability").page());
        assertEquals(AppPage.CREATE_GROUP, read("how do I create a study group?").page());
        assertEquals(AppPage.STUDY_GROUPS, read("take me to my groups").page());
        assertEquals(AppPage.BUDDY_REQUESTS, read("where are my requests").page());
        assertEquals(AppPage.FIND_BUDDIES, read("how do I find a buddy?").page());
        assertEquals(AppPage.NOTIFICATIONS, read("open notifications").page());
        assertEquals(AppPage.FOREST, read("take me to my forest").page());
        assertEquals(AppPage.STUDY_ROOM, read("open quiz buddy").page());
    }

    @Test
    void readsPendingQuestionsAsToDosNotBuddySearches() {
        assertEquals(AssistantIntentType.MY_TO_DO, read("do I have any pending buddy requests?").intent());
        assertEquals(AssistantIntentType.MY_TO_DO, read("anything waiting for me").intent());
    }

    @Test
    void keepsAQuizTopicButNotMyNotes() {
        assertEquals("polymorphism", read("quiz me on polymorphism").topic());
        assertNull(read("quiz me on my slides").topic());
        assertNull(read("Quiz me on My Notes").topic());
    }

    @Test
    void keepsAQuizTopicAsTypedAndLeavesTheCourseOutOfIt() {
        AssistantRequest request = read("Quiz me on IS442 Interfaces");

        assertEquals("IS442", request.courseCode());
        assertEquals("Interfaces", request.topic());
        assertEquals("the Strategy pattern", read("quiz me on the Strategy pattern for IS442?").topic());
        assertNull(read("quiz me on IS442").topic());
    }

    @Test
    void takesOnlyACatalogueCourseOutOfAQuizTopic() {
        AssistantRequest request = read("quiz me on interfaces for IS442");

        assertEquals("IS442", request.courseCode());
        assertEquals("interfaces", request.topic());
        assertEquals("Interfaces", read("quiz me on is 442 Interfaces").topic());
    }

    @Test
    void keepsWordsThatOnlyLookLikeACourseCodeInAQuizTopic() {
        AssistantRequest java = read("quiz me on Java 101");
        AssistantRequest sql = read("Quiz me on SQL 201 joins for IS442");

        assertEquals("Java 101", java.topic());
        assertNull(java.courseCode(), "Java 101 is not a course in the catalogue");
        assertEquals("SQL 201 joins", read("quiz me on SQL 201 joins").topic());
        assertEquals("CS 50 recursion", read("quiz me on CS 50 recursion").topic());
        assertEquals("ZZ999 revision", read("quiz me on ZZ999 revision").topic(), "A code the catalogue does not hold stays");
        assertEquals("SQL 201 joins", sql.topic());
        assertEquals("IS442", sql.courseCode(), "The course is the code the catalogue holds, not the first thing shaped like one");
    }

    @Test
    void answersGreetingsWithTheHelpMessage() {
        assertEquals(AssistantIntentType.UNKNOWN, read("hi pixel!").intent());
        assertEquals(AssistantIntentType.UNKNOWN, read("Thanks").intent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "find me an IS442 buddy who is free on evenings",
            "I need a study partner for exam preparation",
            "plan my revision, exam on 20 Nov, 6 hours a week",
            "how do I bake a cake",
            "what is polymorphism?",
            "hi, can you find me someone free on weekends"})
    void leavesAnythingItCannotFullyReadToTheAi(String message) {
        assertTrue(READER.read(message).isEmpty(), message);
    }
}
