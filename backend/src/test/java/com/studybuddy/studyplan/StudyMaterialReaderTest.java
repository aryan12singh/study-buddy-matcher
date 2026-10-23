package com.studybuddy.studyplan;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyMaterialReaderTest {
    private static final byte[] PDF = "%PDF-1.7\nlecture notes".getBytes(StandardCharsets.US_ASCII);

    private final StudyMaterialReader reader =
            new StudyMaterialReader(new StudyPlanProperties(DataSize.ofBytes(100), 20, 30));

    private static MockMultipartFile upload(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }

    private static MockMultipartFile upload(String filename, String content) {
        return upload(filename, content.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpStatus statusOf(Runnable action) {
        return assertThrows(ApiException.class, action::run).getStatus();
    }

    @Test
    void aPdfIsAttachedAsAFile() {
        StudyMaterial material = reader.read(upload("week3.pdf", PDF), null);

        assertEquals(StudyMaterialSource.UPLOAD, material.source());
        assertEquals("week3.pdf", material.filename());
        assertEquals("application/pdf", material.attachments().getFirst().mediaType());
        assertArrayEquals(PDF, material.attachments().getFirst().data());
        assertEquals("", material.text());
    }

    @Test
    void textAndMarkdownFilesGoIntoThePromptAsText() {
        StudyMaterial text = reader.read(upload("topics.txt", "Polymorphism\n"), null);
        StudyMaterial markdown = reader.read(upload("TOPICS.MD", "# Inheritance"), null);

        assertEquals("Polymorphism", text.text());
        assertTrue(text.attachments().isEmpty());
        assertEquals("# Inheritance", markdown.text());
    }

    @Test
    void aByteOrderMarkIsNotPartOfTheText() {
        assertEquals("Encapsulation", reader.read(upload("topics.txt", "\uFEFFEncapsulation"), null).text());
    }

    @Test
    void pastedTopicsAreTrimmedAndHaveNoFilename() {
        StudyMaterial material = reader.read(null, "  Polymorphism  ");

        assertEquals(StudyMaterialSource.PASTE, material.source());
        assertNull(material.filename());
        assertEquals("Polymorphism", material.text());
    }

    @Test
    void anEmptyFilePartCountsAsNoFile() {
        assertEquals(StudyMaterialSource.PASTE, reader.read(upload("", new byte[0]), "Polymorphism").source());
    }

    @Test
    void onlyTheLastPartOfAPathIsTheFilename() {
        assertEquals("notes.pdf", reader.read(upload("C:\\Users\\me\\notes.pdf", PDF), null).filename());
        assertEquals("notes.pdf", reader.read(upload("folder/notes.pdf", PDF), null).filename());
    }

    @Test
    void aFileRenamedToPdfIsRefused() {
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> reader.read(upload("notes.pdf", "plain words"), null)));
    }

    @Test
    void aPdfRenamedToTextIsRefused() {
        byte[] pdfWithBinary = {'%', 'P', 'D', 'F', '-', 0, 1, 2};
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> reader.read(upload("notes.txt", pdfWithBinary), null)));
    }

    @Test
    void textThatIsNotUtf8IsRefused() {
        byte[] latin1 = {'c', 'a', 'f', (byte) 0xE9};
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> reader.read(upload("notes.txt", latin1), null)));
    }

    @Test
    void otherFileTypesAreRefused() {
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> reader.read(upload("notes.docx", "PK"), null)));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> reader.read(upload("notes", "words"), null)));
    }

    @Test
    void anOversizedFileIsRefused() {
        assertEquals(HttpStatus.CONTENT_TOO_LARGE, statusOf(() -> reader.read(upload("notes.pdf", new byte[101]), null)));
    }

    @Test
    void aTextFileWithTooMuchTextIsRefused() {
        assertEquals(HttpStatus.CONTENT_TOO_LARGE, statusOf(() -> reader.read(upload("notes.txt", "x".repeat(31)), null)));
    }

    @Test
    void anEmptyOrBlankFileIsRefused() {
        assertThrows(InvalidInputException.class, () -> reader.read(upload("notes.pdf", new byte[0]), null));
        assertThrows(InvalidInputException.class, () -> reader.read(upload("notes.txt", "  \n "), null));
    }

    @Test
    void tooManyPastedCharactersAreRefused() {
        InvalidInputException error = assertThrows(InvalidInputException.class, () -> reader.read(null, "x".repeat(21)));
        assertTrue(error.getFieldErrors().containsKey("topicsText"));
    }

    @Test
    void neitherFileNorTopicsIsRefused() {
        assertThrows(InvalidInputException.class, () -> reader.read(null, null));
        assertThrows(InvalidInputException.class, () -> reader.read(null, "   "));
    }

    @Test
    void bothFileAndTopicsIsRefused() {
        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> reader.read(upload("notes.pdf", PDF), "Polymorphism"));
        assertTrue(error.getFieldErrors().containsKey("file"));
    }

    @Test
    void neverPrintsTheMaterial() {
        String printed = reader.read(null, "secret notes").toString();
        assertTrue(!printed.contains("secret"), printed);
    }
}
