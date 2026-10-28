package com.studybuddy.assistant;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantOutputValidatorTest {
    private static final AssistantOutputValidator VALIDATOR = new AssistantOutputValidator(JsonMapper.builder().build());

    private static AssistantRequest valid(String json) {
        return VALIDATOR.validate(json).orElseThrow();
    }

    @Test
    void readsTheIntentAndEveryStatedDetail() {
        AssistantRequest request = valid("""
                {"intent": "FIND_BUDDIES", "courseCode": " IS442 ", "studyGoal": null, "studyMode": "IN_PERSON",
                 "groupSize": "SMALL_GROUP", "minSharedHours": 2, "timeOfDay": "EVENING", "groupName": null,
                 "durationMinutes": null, "examDate": "2026-11-20", "hoursPerWeek": 6, "topic": "polymorphism",
                 "page": "EDIT_PROFILE"}""");

        assertEquals(AssistantIntentType.FIND_BUDDIES, request.intent());
        assertEquals("IS442", request.courseCode());
        assertEquals(StudyMode.IN_PERSON, request.studyMode());
        assertEquals(GroupSizePreference.SMALL_GROUP, request.groupSize());
        assertEquals(2, request.minSharedHours());
        assertEquals(TimeOfDay.EVENING, request.timeOfDay());
        assertEquals(LocalDate.of(2026, 11, 20), request.examDate());
        assertEquals(6, request.hoursPerWeek());
        assertEquals("polymorphism", request.topic());
        assertNull(request.studyGoal());
        assertEquals(AppPage.EDIT_PROFILE, request.page());
        assertNull(request.meetingDate());
    }

    @Test
    void readsTheMeetingDayOfASessionQuestionAndDropsAnUnreadableOne() {
        assertEquals(LocalDate.of(2026, 11, 5),
                valid("{\"intent\": \"SESSION_TOPICS\", \"meetingDate\": \"2026-11-05\"}").meetingDate());
        assertNull(valid("{\"intent\": \"SESSION_TOPICS\", \"meetingDate\": \"Thursday\"}").meetingDate());
        assertEquals(AssistantIntentType.PLAN_SESSION, valid("{\"intent\": \"PLAN_SESSION\"}").intent());
    }

    @Test
    void dropsDetailsThatAreNotTheRightKindOfValueRatherThanGuessing() {
        AssistantRequest request = valid("""
                {"intent": "PLAN_STUDY", "studyMode": "HYBRID", "groupSize": "PAIR", "minSharedHours": 2.5,
                 "durationMinutes": 0, "examDate": "next Friday", "hoursPerWeek": 900, "courseCode": "   ",
                 "page": "SETTINGS"}""");

        assertEquals(AssistantIntentType.PLAN_STUDY, request.intent());
        assertNull(request.studyMode());
        assertNull(request.groupSize());
        assertNull(request.minSharedHours());
        assertNull(request.durationMinutes());
        assertNull(request.examDate());
        assertNull(request.hoursPerWeek());
        assertNull(request.courseCode());
        assertNull(request.page(), "A page the app does not have is dropped");
    }

    @Test
    void rejectsAReplyWithoutAKnownIntent() {
        assertTrue(VALIDATOR.validate(null).isEmpty());
        assertTrue(VALIDATOR.validate("Sure! You want a buddy.").isEmpty());
        assertTrue(VALIDATOR.validate("{\"intent\": \"BOOK_A_ROOM\"}").isEmpty());
        assertTrue(VALIDATOR.validate("{\"courseCode\": \"IS442\"}").isEmpty());
        assertTrue(VALIDATOR.validate("[\"FIND_BUDDIES\"]").isEmpty());
    }
}
