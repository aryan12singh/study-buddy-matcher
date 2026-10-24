package com.studybuddy.ai;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiAttachmentTest {
    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void cannotBeChangedThroughTheArrayItWasBuiltFromOrReturned() {
        byte[] original = bytes("notes");
        AiAttachment attachment = new AiAttachment("text/plain", original);

        original[0] = 'X';
        attachment.data()[1] = 'X';

        assertArrayEquals(bytes("notes"), attachment.data());
    }

    @Test
    void attachmentsWithTheSameContentAreEqual() {
        assertEquals(new AiAttachment("text/plain", bytes("notes")), new AiAttachment("text/plain", bytes("notes")));
        assertEquals(new AiAttachment("text/plain", bytes("notes")).hashCode(),
                new AiAttachment("text/plain", bytes("notes")).hashCode());
    }

    @Test
    void neverPrintsTheContents() {
        String printed = new AiAttachment("text/plain", bytes("secret notes")).toString();

        assertFalse(printed.contains("secret"), printed);
        assertEquals("AiAttachment[mediaType=text/plain, bytes=12]", printed);
    }

    @Test
    void rejectsAMissingMediaTypeOrEmptyFile() {
        assertThrows(IllegalArgumentException.class, () -> new AiAttachment(" ", bytes("notes")));
        assertThrows(IllegalArgumentException.class, () -> new AiAttachment(null, bytes("notes")));
        assertThrows(IllegalArgumentException.class, () -> new AiAttachment("text/plain", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new AiAttachment("text/plain", null));
    }
}
