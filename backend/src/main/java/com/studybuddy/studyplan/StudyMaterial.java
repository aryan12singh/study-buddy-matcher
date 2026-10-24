package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAttachment;

import java.util.List;
import java.util.Objects;

/**
 * Course material a roadmap is planned from, already checked by {@link StudyMaterialReader}: one or
 * more uploaded files, or pasted topics. PDFs travel to the AI as attached files in upload order;
 * text (from {@code .txt} or {@code .md} files, or pasted) goes into the prompt. Held only for one
 * request: of the material itself, only the filenames are kept with the plan.
 *
 * @param filenames the uploaded files' names in upload order; empty for pasted topics
 * @param attachments the PDFs to send to the AI, in upload order
 * @param text the text to place in the prompt; each text file's part starts with its name, and it
 *        is empty when every file is a PDF
 */
public record StudyMaterial(StudyMaterialSource source, List<String> filenames, List<AiAttachment> attachments, String text) {
    static final String PDF_MEDIA_TYPE = "application/pdf";

    public StudyMaterial {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(text, "text");
        filenames = List.copyOf(filenames);
        attachments = List.copyOf(attachments);
    }

    static StudyMaterial uploads(List<String> filenames, List<AiAttachment> pdfs, String text) {
        return new StudyMaterial(StudyMaterialSource.UPLOAD, filenames, pdfs, text);
    }

    static StudyMaterial pastedTopics(String text) {
        return new StudyMaterial(StudyMaterialSource.PASTE, List.of(), List.of(), text);
    }

    /** Never print a student's notes into a log. */
    @Override
    public String toString() {
        return "StudyMaterial[source=" + source + ", filenames=" + filenames + ", attachments=" + attachments.size()
                + ", textCharacters=" + text.length() + "]";
    }
}
