package com.studybuddy.music;

import com.studybuddy.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Lists the ambient sound categories and picks a random sound for one from the sound search
 * service. A missing key or a failed search becomes an {@link AmbientUnavailableException}, so the
 * Study Room can say so instead of failing.
 */
@Service
public class AmbientSoundService {
    private static final Logger LOG = LoggerFactory.getLogger(AmbientSoundService.class);

    private final SoundSearchClient client;
    private final AmbientProperties properties;
    private final AmbientAssembler assembler;
    private final RandomGenerator random;

    public AmbientSoundService(SoundSearchClient client, AmbientProperties properties, AmbientAssembler assembler,
            @Qualifier("ambientRandom") RandomGenerator random) {
        this.client = client;
        this.properties = properties;
        this.assembler = assembler;
        this.random = random;
    }

    public List<AmbientCategoryDto> categories() {
        return properties.categories().stream().map(assembler::toDto).toList();
    }

    /**
     * Picks a random playable sound for the category. {@code excludeId} is the sound already
     * playing, so "try another sound" does not hand back the same one when there is a choice.
     */
    public AmbientSoundDto randomSound(String categoryId, Long excludeId) {
        AmbientCategory category = properties.category(categoryId)
                .orElseThrow(() -> new NotFoundException("No ambient sound category called " + categoryId));
        if (!properties.ready()) {
            throw new AmbientUnavailableException("Ambient sounds are not set up on this server");
        }
        List<AmbientSound> sounds = search(category);
        List<AmbientSound> others = sounds.stream().filter(sound -> excludeId == null || sound.id() != excludeId).toList();
        List<AmbientSound> choices = others.isEmpty() ? sounds : others;
        if (choices.isEmpty()) {
            throw new AmbientUnavailableException("No " + category.label().toLowerCase() + " sounds are available right now");
        }
        return assembler.toDto(category, choices.get(random.nextInt(choices.size())));
    }

    private List<AmbientSound> search(AmbientCategory category) {
        try {
            return client.search(category.query());
        } catch (IOException failure) {
            LOG.warn("Ambient sound search failed for {}: {}", category.id(), failure.getMessage());
            throw new AmbientUnavailableException("Ambient sounds are unavailable right now; try again later");
        }
    }
}
