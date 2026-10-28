package com.studybuddy.student;

import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmuSchoolTest {
    @Test void everySmuSchoolCodeIsAcceptedInAnyCase() {
        assertEquals(7, SmuSchool.values().length);
        assertEquals("SCIS", SmuSchool.code("SCIS"));
        assertEquals("LKCSB", SmuSchool.code(" lkcsb "));
        assertEquals("CIS", SmuSchool.code("cis"));
    }
    @Test void otherSchoolsAreRejectedWithAFieldError() {
        for (String value : new String[] {null, "", "SOB", "NUS", "School of Computing"}) {
            var error = assertThrows(InvalidInputException.class, () -> SmuSchool.code(value), "School: " + value);
            assertTrue(error.getFieldErrors().containsKey("school"));
        }
    }
}
