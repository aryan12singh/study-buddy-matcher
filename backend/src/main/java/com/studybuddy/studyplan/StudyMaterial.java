package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAttachment;

import java.util.List;
import java.util.Objects;

/**
 * Course material a roadmap is planned from, already checked by {@link StudyMaterialReader}. A PDF
 * travels to the AI as an attached file; text (an uploaded {@code .txt} or {@code .md} file, or
 * pasted topics) goes into the prompt. Held only for one request: of the material itself, only the
 * filename is kept with the plan.
 *
 * @param filename the uploaded file's name, or null for pasted topics
 * @param attachments the files to send to the AI; empty for text material
 * @param text the text to place in the prompt; empty for a PDF
 */
public record StudyMaterial(StudyMaterialSource source, String filename, List<AiAttachment> attachments, String text) {
    static final String PDF_MEDIA_TYPE = "application/pdf";

    public StudyMaterial {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(text, "text");
        attachments = List.copyOf(attachments);
    }

    static StudyMaterial pdf(String filename, byte[] content) {
        return new StudyMaterial(StudyMaterialSource.UPLOAD, filename,
                List.of(new AiAttachment(PDF_MEDIA_TYPE, content)), "");
    }

    static StudyMaterial textFile(String filename, String text) {
        return new StudyMaterial(StudyMaterialSource.UPLOAD, filename, List.of(), text);
    }

    static StudyMaterial pastedTopics(String text) {
        return new StudyMaterial(StudyMaterialSource.PASTE, null, List.of(), text);
    }

    /** Never print a student's notes into a log. */
    @Override
    public String toString() {
        return "StudyMaterial[source=" + source + ", filename=" + filename + ", attachments=" + attachments.size()
                + ", textCharacters=" + text.length() + "]";
    }
}
