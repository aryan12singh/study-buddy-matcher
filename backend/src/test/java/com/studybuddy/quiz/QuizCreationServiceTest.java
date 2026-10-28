package com.studybuddy.quiz;

import com.studybuddy.studyplan.StudyMaterial;
import com.studybuddy.studyplan.StudyMaterialSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizCreationServiceTest {
    private static StudyMaterial files(String... names) {
        return new StudyMaterial(StudyMaterialSource.UPLOAD, List.of(names), List.of(), "text");
    }

    @Test
    void titlesAQuizByItsFocusFirst() {
        assertEquals("Interfaces", QuizCreationService.titleOf("Interfaces", files("week4.pdf")));
    }

    @Test
    void titlesAQuizWithoutAFocusByItsFilesOrAsPastedNotes() {
        assertEquals("week4.pdf, week5.md", QuizCreationService.titleOf(null, files("week4.pdf", "week5.md")));
        assertEquals(QuizCreationService.PASTED_NOTES_TITLE, QuizCreationService.titleOf(null,
                new StudyMaterial(StudyMaterialSource.PASTE, List.of(), List.of(), "notes")));
    }

    @Test
    void cutsALongListOfFileNamesToFitTheColumn() {
        String title = QuizCreationService.titleOf(null, files("a".repeat(200) + ".pdf", "b".repeat(200) + ".pdf"));

        assertEquals(Quiz.TITLE_LIMIT, title.length());
        assertTrue(title.endsWith("…"));
    }
}
