package com.studybuddy.studysession;

import com.studybuddy.matching.SharedSlotDto;
import java.util.List;

public record SessionAvailabilityDto(String timeZone, int participantCount, List<SharedSlotDto> sharedSlots, long sharedMinutes) { }
