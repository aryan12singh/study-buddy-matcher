package com.studybuddy.music;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreesoundClientTest {
    @Test void parseKeepsPlayableResultsOnly() throws Exception {
        String reply = """
                {"count": 2, "results": [
                  {"id": 1, "name": "Rain", "username": "ana", "license": "http://creativecommons.org/publicdomain/zero/1.0/",
                   "previews": {"preview-hq-mp3": "https://cdn/1.mp3"}},
                  {"id": 2, "name": "Silent", "username": "ben", "license": "x", "previews": {}}
                ]}""";
        assertEquals(List.of(new AmbientSound(1, "Rain", "ana", "http://creativecommons.org/publicdomain/zero/1.0/", "https://cdn/1.mp3")),
                FreesoundClient.parse(reply));
    }

    @Test void parseWithNoResultsIsEmpty() throws Exception {
        assertEquals(List.of(), FreesoundClient.parse("{\"count\": 0, \"results\": []}"));
    }

    @Test void unreadableReplyIsAnIoFailure() {
        assertThrows(IOException.class, () -> FreesoundClient.parse("not json"));
        assertThrows(IOException.class, () -> FreesoundClient.parse(null));
    }
}
