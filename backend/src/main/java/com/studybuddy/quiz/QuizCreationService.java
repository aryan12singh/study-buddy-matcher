package com.studybuddy.quiz;

import com.studybuddy.studyplan.StudyMaterial;
import com.studybuddy.studyplan.StudyMaterialReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Starts a quiz from the student's uploaded or pasted material in three steps: a short database
 * read that checks the request, the AI call, and a short database write. Deliberately not
 * transactional: Gemini can take many seconds, and a transaction held open that long would tie up a
 * database connection. The material is sent to Gemini for this one request and never stored.
 */
@Service
public class QuizCreationService {
    static final String PASTED_NOTES_TITLE = "Pasted notes";

    private final QuizService quizzes;
    private final StudyMaterialReader materials;
    private final QuizWriter writer;

    public QuizCreationService(QuizService quizzes, StudyMaterialReader materials, QuizWriter writer) {
        this.quizzes = quizzes;
        this.materials = materials;
        this.writer = writer;
    }

    public QuizDto startForGroup(Long groupId, Long actorId, StartQuizInput input, List<MultipartFile> files) {
        QuizStartRequest request = quizzes.prepareGroupStart(groupId, actorId, input);
        StudyMaterial material = materials.read(files, input.notesText());
        QuizDraft draft = writer.write(actorId, request.course(), request.focus(), material);
        return quizzes.saveGroupStarted(actorId, request, titleOf(request.focus(), material), draft);
    }

    public QuizDto startPersonal(Long actorId, PersonalQuizInput input, List<MultipartFile> files) {
        QuizStartRequest request = quizzes.preparePersonalStart(actorId, input);
        StudyMaterial material = materials.read(files, input.notesText());
        QuizDraft draft = writer.write(actorId, request.course(), request.focus(), material);
        return quizzes.savePersonalStarted(actorId, request, titleOf(request.focus(), material), draft);
    }

    /** What quiz lists show: the focus if given, otherwise the files' names, cut to fit the column. */
    static String titleOf(String focus, StudyMaterial material) {
        String title = focus != null ? focus
                : material.filenames().isEmpty() ? PASTED_NOTES_TITLE : String.join(", ", material.filenames());
        return title.length() <= Quiz.TITLE_LIMIT ? title : title.substring(0, Quiz.TITLE_LIMIT - 1) + "…";
    }
}
