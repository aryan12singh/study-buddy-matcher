package com.studybuddy.assistant;

/** One thing waiting for the student, with a button to the page where they can deal with it. */
public record AssistantTodoDto(String text, String label, String link) {
}
