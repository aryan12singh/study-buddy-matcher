package com.studybuddy.studyplan;

import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.InvalidInputException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns an upload or pasted topics into checked {@link StudyMaterial}. A file's type is decided by
 * its extension and its contents together, so a renamed Word document or image is refused rather
 * than sent to the AI: a {@code .pdf} must start with the PDF signature, and a {@code .txt} or
 * {@code .md} file must be readable UTF-8 text.
 */
@Component
public class StudyMaterialReader {
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String BYTE_ORDER_MARK = "﻿";

    private final StudyPlanProperties properties;

    public StudyMaterialReader(StudyPlanProperties properties) {
        this.properties = properties;
    }

    /** Exactly one of the two is expected: a file with a name, or non-blank topics. */
    public StudyMaterial read(MultipartFile file, String topicsText) {
        boolean fileSent = file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank();
        boolean topicsSent = topicsText != null && !topicsText.isBlank();
        if (fileSent && topicsSent) {
            throw new InvalidInputException("file", "Upload a file or paste topics, not both");
        }
        if (fileSent) {
            return readFile(file);
        }
        if (topicsSent) {
            return readTopics(topicsText);
        }
        throw new InvalidInputException("topicsText", "Upload a file or paste your topics");
    }

    private StudyMaterial readTopics(String topicsText) {
        String text = topicsText.strip();
        if (text.length() > properties.maxTopicsCharacters()) {
            throw new InvalidInputException("topicsText",
                    "Topics must be at most " + properties.maxTopicsCharacters() + " characters");
        }
        return StudyMaterial.pastedTopics(text);
    }

    private StudyMaterial readFile(MultipartFile file) {
        String filename = filenameOf(file);
        if (file.getSize() > properties.maxUploadSize().toBytes()) {
            throw new StudyMaterialTooLargeException(
                    "The file must be at most " + properties.maxUploadSize().toMegabytes() + "MB");
        }
        byte[] content = contentOf(file);
        if (content.length == 0) {
            throw new InvalidInputException("file", "The file is empty");
        }
        String extension = filename.toLowerCase(Locale.ROOT);
        if (extension.endsWith(".pdf")) {
            if (!startsWithPdfSignature(content)) {
                throw new UnsupportedStudyMaterialException();
            }
            return StudyMaterial.pdf(filename, content);
        }
        if (extension.endsWith(".txt") || extension.endsWith(".md")) {
            return readTextFile(filename, content);
        }
        throw new UnsupportedStudyMaterialException();
    }

    private StudyMaterial readTextFile(String filename, byte[] content) {
        String text = utf8TextOf(content).orElseThrow(UnsupportedStudyMaterialException::new).strip();
        if (text.isEmpty()) {
            throw new InvalidInputException("file", "The file is empty");
        }
        if (text.length() > properties.maxTextFileCharacters()) {
            throw new StudyMaterialTooLargeException(
                    "The file must hold at most " + properties.maxTextFileCharacters() + " characters of text");
        }
        return StudyMaterial.textFile(filename, text);
    }

    /** Browsers may send a full path; only the last part is the name, and it must fit its column. */
    private static String filenameOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).strip();
        if (name.isEmpty() || name.length() > InputRules.TEXT_LIMIT) {
            throw new InvalidInputException("file", "The file name must be 1 to " + InputRules.TEXT_LIMIT + " characters");
        }
        return name;
    }

    private static byte[] contentOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException failure) {
            throw new InvalidInputException("file", "The file could not be read; upload it again");
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
