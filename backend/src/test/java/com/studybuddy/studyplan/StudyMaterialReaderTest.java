package com.studybuddy.studyplan;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyMaterialReaderTest {
    private static final byte[] PDF = "%PDF-1.7\nlecture notes".getBytes(StandardCharsets.US_ASCII);

    /**
     * Files add up to at most 100 bytes, at most 3 files, 20 pasted characters, and 80 characters
     * of text-file content, counting each file's "File:" heading.
     */
    private final StudyMaterialReader reader =
            new StudyMaterialReader(new StudyPlanProperties(DataSize.ofBytes(100), 3, 20, 80, 40, 5, 5, 16, 40, "Asia/Singapore"));

    private static MockMultipartFile upload(String filename, byte[] content) {
        return new MockMultipartFile("files", filename, "application/octet-stream", content);
    }

    private static MockMultipartFile upload(String filename, String content) {
        return upload(filename, content.getBytes(StandardCharsets.UTF_8));
    }

    private StudyMaterial read(MultipartFile... files) {
        return reader.read(List.of(files), null);
    }

    private static HttpStatus statusOf(Runnable action) {
        return assertThrows(ApiException.class, action::run).getStatus();
    }

    @Test
    void aPdfIsAttachedAsAFile() {
        StudyMaterial material = read(upload("week3.pdf", PDF));

        assertEquals(StudyMaterialSource.UPLOAD, material.source());
        assertEquals(List.of("week3.pdf"), material.filenames());
        assertEquals("application/pdf", material.attachments().getFirst().mediaType());
        assertArrayEquals(PDF, material.attachments().getFirst().data());
        assertEquals("", material.text());
    }

    @Test
    void severalPdfsAreAttachedInUploadOrder() {
        byte[] second = "%PDF-1.7\nweek four".getBytes(StandardCharsets.US_ASCII);

        StudyMaterial material = read(upload("week3.pdf", PDF), upload("week4.pdf", second));

        assertEquals(List.of("week3.pdf", "week4.pdf"), material.filenames());
        assertArrayEquals(PDF, material.attachments().get(0).data());
        assertArrayEquals(second, material.attachments().get(1).data());
    }

    @Test
    void textFilesAreJoinedEachStartingWithItsName() {
        StudyMaterial material = read(upload("week3.txt", "Polymorphism\n"), upload("WEEK4.MD", "# Generics"));

        assertEquals("File: week3.txt\nPolymorphism\n\nFile: WEEK4.MD\n# Generics", material.text());
        assertTrue(material.attachments().isEmpty());
    }

    @Test
    void pdfsAndTextFilesCanBeMixed() {
        StudyMaterial material = read(upload("week3.pdf", PDF), upload("notes.txt", "Polymorphism"));

        assertEquals(List.of("week3.pdf", "notes.txt"), material.filenames());
        assertEquals(1, material.attachments().size());
        assertEquals("File: notes.txt\nPolymorphism", material.text());
    }

    @Test
    void aByteOrderMarkIsNotPartOfTheText() {
        assertEquals("File: topics.txt\nEncapsulation", read(upload("topics.txt", "\uFEFFEncapsulation")).text());
    }

    @Test
    void pastedTopicsAreTrimmedAndHaveNoFiles() {
        StudyMaterial material = reader.read(null, "  Polymorphism  ");

        assertEquals(StudyMaterialSource.PASTE, material.source());
        assertTrue(material.filenames().isEmpty());
        assertEquals("Polymorphism", material.text());
    }

    @Test
    void anEmptyFilePartCountsAsNoFile() {
        assertEquals(StudyMaterialSource.PASTE, reader.read(List.of(upload("", new byte[0])), "Polymorphism").source());
    }

    @Test
    void onlyTheLastPartOfAPathIsTheFilename() {
        assertEquals(List.of("notes.pdf"), read(upload("C:\\Users\\me\\notes.pdf", PDF)).filenames());
        assertEquals(List.of("notes.pdf"), read(upload("folder/notes.pdf", PDF)).filenames());
    }

    @Test
    void aFileRenamedToPdfIsRefused() {
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> read(upload("notes.pdf", "plain words"))));
    }

    @Test
    void aPdfRenamedToTextIsRefused() {
        byte[] pdfWithBinary = {'%', 'P', 'D', 'F', '-', 0, 1, 2};
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> read(upload("notes.txt", pdfWithBinary))));
    }

    @Test
    void textThatIsNotUtf8IsRefused() {
        byte[] latin1 = {'c', 'a', 'f', (byte) 0xE9};
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> read(upload("notes.txt", latin1))));
    }

    @Test
    void otherFileTypesAreRefusedEvenAlongsideGoodOnes() {
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> read(upload("notes.docx", "PK"))));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> read(upload("notes", "words"))));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusOf(() -> read(upload("week3.pdf", PDF), upload("slides.pptx", "PK"))));
    }

    @Test
    void filesThatAddUpToTooMuchAreRefused() {
        assertEquals(HttpStatus.CONTENT_TOO_LARGE, statusOf(() -> read(upload("notes.pdf", new byte[101]))));
        byte[] sixty = new byte[60];
        System.arraycopy(PDF, 0, sixty, 0, PDF.length);
        assertEquals(HttpStatus.CONTENT_TOO_LARGE, statusOf(() -> read(upload("a.pdf", sixty), upload("b.pdf", sixty))));
    }

    @Test
    void tooManyFilesAreRefused() {
        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> read(upload("a.txt", "A"), upload("b.txt", "B"), upload("c.txt", "C"), upload("d.txt", "D")));
        assertTrue(error.getFieldErrors().containsKey("files"));
    }

    @Test
    void textFilesHoldingTooMuchTextTogetherAreRefused() {
        // Each file fits alone (12-character heading plus 30), but together they hold 86 characters.
        assertEquals(HttpStatus.CONTENT_TOO_LARGE, statusOf(() -> read(upload("a.txt", "x".repeat(30)), upload("b.txt", "y".repeat(30)))));
    }

    @Test
    void anEmptyOrBlankFileIsRefused() {
        assertThrows(InvalidInputException.class, () -> read(upload("notes.pdf", new byte[0])));
        assertThrows(InvalidInputException.class, () -> read(upload("notes.txt", "  \n ")));
    }

    @Test
    void tooManyPastedCharactersAreRefused() {
        InvalidInputException error = assertThrows(InvalidInputException.class, () -> reader.read(null, "x".repeat(21)));
        assertTrue(error.getFieldErrors().containsKey("topicsText"));
    }

    @Test
    void neitherFilesNorTopicsIsRefused() {
        assertThrows(InvalidInputException.class, () -> reader.read(null, null));
        assertThrows(InvalidInputException.class, () -> reader.read(List.of(), "   "));
    }

    @Test
    void bothFilesAndTopicsIsRefused() {
        InvalidInputException error = assertThrows(InvalidInputException.class,
                () -> reader.read(List.of(upload("notes.pdf", PDF)), "Polymorphism"));
        assertTrue(error.getFieldErrors().containsKey("files"));
    }

    @Test
    void neverPrintsTheMaterial() {
        String printed = reader.read(null, "secret notes").toString();
        assertFalse(printed.contains("secret"), printed);
    }
}
