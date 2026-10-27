package com.studybuddy.quiz;

import jakarta.validation.constraints.NotNull;

/**
 * The form fields sent with a practice quiz's files, as multipart form fields beside the files. The
 * material is either the files or {@code notesText}.
 *
 * @param focus what to ask mostly about, or blank to cover the whole material
 */
public record PersonalQuizInput(@NotNull(message = "Choose a course") Long courseId, String notesText, String focus) {
}
