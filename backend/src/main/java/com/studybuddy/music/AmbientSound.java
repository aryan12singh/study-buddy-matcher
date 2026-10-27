package com.studybuddy.music;

/** One loopable sound returned by the provider. */
public record AmbientSound(long id, String name, String author, String license, String previewUrl) {
}
