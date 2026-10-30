package com.studybuddy.studysession;

import com.studybuddy.matching.SharedSlotDto;
import java.util.List;

/**
 * The weekly times everyone in a group or connection is free, in Singapore time, with the total
 * shared minutes.
 */
public record SessionAvailabilityDto(String timeZone, int participantCount, List<SharedSlotDto> sharedSlots, long sharedMinutes) { }
