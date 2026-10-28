package com.studybuddy.assistant;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickIntentReaderTest {
    private static final QuickIntentReader READER = new QuickIntentReader();

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
