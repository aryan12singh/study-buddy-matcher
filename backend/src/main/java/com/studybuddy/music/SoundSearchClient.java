package com.studybuddy.music;

import java.io.IOException;
import java.util.List;

/** Finds playable sounds for a search phrase from an external provider. */
public interface SoundSearchClient {
    List<AmbientSound> search(String query) throws IOException;
}
