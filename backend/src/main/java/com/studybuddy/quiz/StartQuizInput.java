package com.studybuddy.quiz;

/**
 * The form fields sent with a group quiz's files, as multipart form fields beside the files. The
 * material is either the files or {@code notesText}; {@link com.studybuddy.studyplan.StudyMaterialReader}
 * checks it, and the service checks the focus's configurable length.
 *
 * @param focus what to ask mostly about, or blank to cover the whole material
 * @param sessionId the group's session the quiz belongs to, or null for one started from the room
 */
public record StartQuizInput(String notesText, String focus, Long sessionId) {
}
