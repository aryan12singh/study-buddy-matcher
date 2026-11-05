package com.studybuddy.music;

/** One ambient sound to play, with the author and licence the Study Room must show beside it. */
public record AmbientSoundDto(String categoryId, long id, String name, String author, String license, String previewUrl) {
}
