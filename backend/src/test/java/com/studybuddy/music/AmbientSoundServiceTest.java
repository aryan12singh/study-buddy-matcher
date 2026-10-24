package com.studybuddy.music;

import com.studybuddy.common.error.NotFoundException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class AmbientSoundServiceTest {
    private final SoundSearchClient client = mock(SoundSearchClient.class);
    private final RandomGenerator random = mock(RandomGenerator.class);
    private final AmbientSound first = new AmbientSound(1, "Light rain", "ana", "https://licence/1", "https://cdn/1.mp3");
    private final AmbientSound second = new AmbientSound(2, "Heavy rain", "ben", "https://licence/2", "https://cdn/2.mp3");

    private AmbientSoundService serviceWithKey(String apiKey) {
        var properties = new AmbientProperties("https://freesound.org/apiv2", apiKey, Duration.ofSeconds(5), 15, 30, 600, "Creative Commons 0",
                List.of(new AmbientCategory("rain", "Rain", "rain ambience")));
        return new AmbientSoundService(client, properties, new AmbientAssembler(), random);
    }

    @Test void categoriesListIdAndLabelOnly() {
        assertEquals(List.of(new AmbientCategoryDto("rain", "Rain")), serviceWithKey("key").categories());
    }

    @Test void randomSoundSearchesTheCategoryQuery() throws Exception {
        when(client.search("rain ambience")).thenReturn(List.of(first, second));
        when(random.nextInt(2)).thenReturn(1);
        assertEquals(new AmbientSoundDto("rain", 2, "Heavy rain", "ben", "https://licence/2", "https://cdn/2.mp3"),
                serviceWithKey("key").randomSound("rain", null));
    }

    @Test void excludedSoundIsSkippedWhenThereIsAnotherChoice() throws Exception {
        when(client.search("rain ambience")).thenReturn(List.of(first, second));
        when(random.nextInt(1)).thenReturn(0);
        assertEquals(2, serviceWithKey("key").randomSound("rain", 1L).id());
    }

    @Test void excludedSoundIsReusedWhenItIsTheOnlyOne() throws Exception {
        when(client.search("rain ambience")).thenReturn(List.of(first));
        when(random.nextInt(1)).thenReturn(0);
        assertEquals(1, serviceWithKey("key").randomSound("rain", 1L).id());
    }

    @Test void unknownCategoryIsNotFound() {
        assertThrows(NotFoundException.class, () -> serviceWithKey("key").randomSound("traffic", null));
        verifyNoInteractions(client);
    }

    @Test void missingKeyReportsUnavailable() {
        assertThrows(AmbientUnavailableException.class, () -> serviceWithKey(" ").randomSound("rain", null));
        verifyNoInteractions(client);
    }

    @Test void noResultsOrProviderFailureReportsUnavailable() throws Exception {
        when(client.search("rain ambience")).thenReturn(List.of()).thenThrow(new IOException("down"));
        var service = serviceWithKey("key");
        assertThrows(AmbientUnavailableException.class, () -> service.randomSound("rain", null));
        assertThrows(AmbientUnavailableException.class, () -> service.randomSound("rain", null));
        verify(random, never()).nextInt(anyInt());
    }
}
