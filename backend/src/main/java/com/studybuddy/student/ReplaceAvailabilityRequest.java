package com.studybuddy.student;

import java.util.List;

/** A student's full weekly availability; an empty list clears it. */
public record ReplaceAvailabilityRequest(List<AvailabilitySlotDto> slots) {
}
