package com.studybuddy.common.http;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;

/**
 * Reads JSON text as usual but refuses the NUL character (U+0000). PostgreSQL cannot
 * store it in a text column, so it would otherwise fail deep in a save; refused here, every text
 * field in every request body gets the same 400.
 */
class TextWithoutNulDeserializer extends StringDeserializer {
    private static final char NUL = '\u0000';

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String text = super.deserialize(parser, context);
        if (text != null && text.indexOf(NUL) >= 0) {
            throw context.weirdStringException(text, String.class, "Text cannot contain the NUL character");
        }
        return text;
    }
}
