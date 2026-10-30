package com.studybuddy.ai;

import com.studybuddy.common.error.InternalInvariantException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slot filling in one pass, loud failure on a missing slot or template, and line-ending normalisation. */
class PromptTemplateTest {

    @Test
    void fillsEverySlotAndLeavesOtherTextAlone() {
        PromptTemplate template = PromptTemplate.of("test", "Course {{course}}, {{count}} questions on {{course}}. {not a slot}");

        assertEquals("Course IS442, 10 questions on IS442. {not a slot}", template.fill(Map.of("course", "IS442", "count", "10")));
    }

    @Test
    void neverSearchesAValueForSlots() {
        PromptTemplate template = PromptTemplate.of("test", "<message>{{message}}</message> today is {{today}}");

        assertEquals("<message>{{today}} costs $1 \\ more</message> today is Monday",
                template.fill(Map.of("message", "{{today}} costs $1 \\ more", "today", "Monday")));
    }

    @Test
    void aSlotWithoutAValueFailsLoudlyAsAnInternalFault() {
        PromptTemplate template = PromptTemplate.of("prompts/test.txt", "Hello {{name}}");

        InternalInvariantException failure = assertThrows(InternalInvariantException.class, () -> template.fill(Map.of()));
        assertTrue(failure.getMessage().contains("{{name}}"), failure.getMessage());
        assertTrue(failure.getMessage().contains("prompts/test.txt"), failure.getMessage());
    }

    @Test
    void windowsLineEndingsReadTheSameEverywhere() {
        assertEquals("one\ntwo", PromptTemplate.of("test", "one\r\ntwo").fill(Map.of()));
    }

    @Test
    void loadsAShippedTemplateFromTheClasspath() {
        String prompt = PromptTemplate.load("prompts/match-explanation-v5.txt").fill(Map.of("evidence", "{}"));

        assertFalse(prompt.contains("{{"), prompt);
        assertFalse(prompt.contains("\r"), prompt);
    }

    @Test
    void aMissingTemplateFailsLoudly() {
        assertThrows(InternalInvariantException.class, () -> PromptTemplate.load("prompts/no-such-template.txt"));
    }
}
