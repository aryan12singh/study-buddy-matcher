package com.studybuddy.ai;

import java.util.Arrays;
import java.util.Objects;

/**
 * A file sent to the AI alongside the prompt, such as a student's lecture notes. Held only for one
 * request and never stored. Immutable: the bytes are copied in and out, so a caller cannot change
 * them after the attachment is built.
 */
public record AiAttachment(String mediaType, byte[] data) {

    public AiAttachment {
        if (mediaType == null || mediaType.isBlank()) {
            throw new IllegalArgumentException("An attachment needs a media type");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("An attachment cannot be empty");
        }
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    /** Records compare arrays by reference; two attachments with the same content are equal. */
    @Override
    public boolean equals(Object other) {
        return other instanceof AiAttachment attachment
                && mediaType.equals(attachment.mediaType) && Arrays.equals(data, attachment.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaType, Arrays.hashCode(data));
    }

    /** Never print a student's file contents into a log. */
    @Override
    public String toString() {
        return "AiAttachment[mediaType=" + mediaType + ", bytes=" + data.length + "]";
    }
}
