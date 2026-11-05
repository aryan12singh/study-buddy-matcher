package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAttachment;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

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
    /** A marker inside the student's text or file names would end the material block early. */
    private static final Pattern MATERIAL_MARKER = Pattern.compile("</?material>", Pattern.CASE_INSENSITIVE);

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

    /** Removes {@code <material>} markers from text the student wrote, so it stays inside its block. */
    public static String withoutMarkers(String text) {
        return MATERIAL_MARKER.matcher(text).replaceAll("");
    }

    /** The text between {@code <material>} markers for the prompt; empty when every file is attached. */
    public String promptBlock() {
        return text.isEmpty() ? "" : "<material>\n" + withoutMarkers(text) + "\n</material>";
    }

    /**
     * Tells the AI where the material is: attached PDFs, text in the block, or both, with files
     * named in upload order.
     *
     * @param pasted what pasted material is, such as "notes the student pasted"
     * @param stressOrder whether to say the files come in the student's order, for a roadmap that
     *        follows the teaching order
     */
    public String describeSource(String pasted, boolean stressOrder) {
        if (source == StudyMaterialSource.PASTE) {
            return "the " + pasted + " between <material> and </material> below";
        }
        String files = "these files" + (stressOrder ? ", in the order the student gave them" : "") + ": "
                + withoutMarkers(String.join(", ", filenames)) + ".";
        String attached = stressOrder ? " attached in that order" : " attached";
        boolean hasPdf = !attachments.isEmpty();
        boolean hasText = !text.isEmpty();
        if (hasPdf && hasText) {
            return files + " The PDF files are" + attached + ", and the text of each .txt or .md file is"
                    + " between <material> and </material> below, starting with \"File:\" and its name";
        }
        return hasPdf ? files + " They are" + attached
                : files + " Their text is between <material> and </material> below, each starting with \"File:\" and its name";
    }

    /** Never print a student's notes into a log. */
    @Override
    public String toString() {
        return "StudyMaterial[source=" + source + ", filenames=" + filenames + ", attachments=" + attachments.size()
                + ", textCharacters=" + text.length() + "]";
    }
}
