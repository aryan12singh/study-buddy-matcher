package com.studybuddy.music;

import org.springframework.stereotype.Component;

/** Builds the ambient sound DTOs from the configured categories and the sounds found for them. */
@Component
public class AmbientAssembler {
    public AmbientCategoryDto toDto(AmbientCategory category) {
        return new AmbientCategoryDto(category.id(), category.label());
    }

    public AmbientSoundDto toDto(AmbientCategory category, AmbientSound sound) {
        return new AmbientSoundDto(category.id(), sound.id(), sound.name(), sound.author(), sound.license(), sound.previewUrl());
    }
}
