package com.studybuddy.profile;

/**
 * A student's profile as another student is allowed to see it. Which of the
 * two shapes a viewer receives is decided by {@link ProfileViewAssembler}
 * alone; sealing the interface keeps a third, unchecked shape from appearing.
 */
public sealed interface ProfileDto permits PublicProfileDto, ConnectedProfileDto {

    Long id();
}
