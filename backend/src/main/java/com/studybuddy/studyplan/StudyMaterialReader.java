package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAttachment;
import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.InvalidInputException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns uploads or pasted topics into checked {@link StudyMaterial}. A student may upload several
 * files, such as one week's slides each, within a count and a total size. Each file's type is
 * decided by its extension and its contents together, so a renamed Word document or image is
 * refused rather than sent to the AI: a {@code .pdf} must start with the PDF signature, and a
 * {@code .txt} or {@code .md} file must be readable UTF-8 text.
 */
@Component
public class StudyMaterialReader {
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String BYTE_ORDER_MARK = "﻿";

    private final StudyPlanProperties properties;

    public StudyMaterialReader(StudyPlanProperties properties) {
        this.properties = properties;
    }

    /** Files or non-blank topics, not both. A file part with no name is an empty form field. */
    public StudyMaterial read(List<MultipartFile> files, String topicsText) {
        List<MultipartFile> sent = files == null ? List.of() : files.stream()
                .filter(file -> file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                .toList();
        boolean topicsSent = topicsText != null && !topicsText.isBlank();
        if (!sent.isEmpty() && topicsSent) {
            throw new InvalidInputException("files", "Upload files or paste topics, not both");
        }
        if (!sent.isEmpty()) {
            return readFiles(sent);
        }
        if (topicsSent) {
            return readTopics(topicsText);
        }
        throw new InvalidInputException("topicsText", "Upload your slides or notes, or paste your topics");
    }

    private StudyMaterial readTopics(String topicsText) {
        String text = topicsText.strip();
        if (text.length() > properties.maxTopicsCharacters()) {
            throw new InvalidInputException("topicsText",
                    "Topics must be at most " + properties.maxTopicsCharacters() + " characters");
        }
        return StudyMaterial.pastedTopics(text);
    }

    private StudyMaterial readFiles(List<MultipartFile> files) {
        if (files.size() > properties.maxFiles()) {
            throw new InvalidInputException("files", "Upload at most " + properties.maxFiles() + " files");
        }
        long totalBytes = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalBytes > properties.maxUploadSize().toBytes()) {
            throw new StudyMaterialTooLargeException(
                    "The files must add up to at most " + properties.maxUploadSize().toMegabytes() + "MB");
        }
        List<String> filenames = new ArrayList<>();
        List<AiAttachment> pdfs = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = filenameOf(file);
            byte[] content = contentOf(file);
            if (content.length == 0) {
                throw new InvalidInputException("files", filename + " is empty");
            }
            String extension = filename.toLowerCase(Locale.ROOT);
            if (extension.endsWith(".pdf") && startsWithPdfSignature(content)) {
                pdfs.add(new AiAttachment(StudyMaterial.PDF_MEDIA_TYPE, content));
            } else if (extension.endsWith(".txt") || extension.endsWith(".md")) {
                textParts.add("File: " + filename + "\n" + textOf(filename, content));
            } else {
                throw new UnsupportedStudyMaterialException();
            }
            filenames.add(filename);
        }
        String text = String.join("\n\n", textParts);
        if (text.length() > properties.maxTextFileCharacters()) {
            throw new StudyMaterialTooLargeException(
                    "Text files must hold at most " + properties.maxTextFileCharacters() + " characters in total");
        }
        return StudyMaterial.uploads(filenames, pdfs, text);
    }

    private static String textOf(String filename, byte[] content) {
        String text = utf8TextOf(content).orElseThrow(UnsupportedStudyMaterialException::new).strip();
        if (text.isEmpty()) {
            throw new InvalidInputException("files", filename + " is empty");
        }
        return text;
    }

    /** Browsers may send a full path; only the last part is the name, and it must fit its column. */
    private static String filenameOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).strip();
        if (name.isEmpty() || name.length() > InputRules.TEXT_LIMIT) {
            throw new InvalidInputException("files", "File names must be 1 to " + InputRules.TEXT_LIMIT + " characters");
        }
        return name;
    }

    private static byte[] contentOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException failure) {
            throw new InvalidInputException("files", "A file could not be read; upload it again");
        }
    }

    private static boolean startsWithPdfSignature(byte[] content) {
        return content.length >= PDF_SIGNATURE.length
                && Arrays.equals(content, 0, PDF_SIGNATURE.length, PDF_SIGNATURE, 0, PDF_SIGNATURE.length);
    }

    /** Strict UTF-8 with no NUL characters; anything else is a binary file with a text extension. */
    private static Optional<String> utf8TextOf(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            if (text.indexOf('\0') >= 0) {
                return Optional.empty();
            }
            return Optional.of(text.startsWith(BYTE_ORDER_MARK) ? text.substring(1) : text);
        } catch (CharacterCodingException notText) {
            return Optional.empty();
        }
    }
}
