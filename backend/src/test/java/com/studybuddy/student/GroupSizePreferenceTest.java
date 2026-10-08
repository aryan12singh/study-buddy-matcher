package com.studybuddy.student;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupSizePreferenceTest {
    private static final int LARGEST_GROUP = 5;

    @Test void eachChoiceStoresItsRange() {
        assertEquals(2, GroupSizePreference.ONE_TO_ONE.minimumSize());
        assertEquals(2, GroupSizePreference.ONE_TO_ONE.maximumSize(LARGEST_GROUP));
        assertEquals(3, GroupSizePreference.SMALL_GROUP.minimumSize());
        assertEquals(LARGEST_GROUP, GroupSizePreference.SMALL_GROUP.maximumSize(LARGEST_GROUP));
        assertEquals(2, GroupSizePreference.EITHER.minimumSize());
        assertEquals(LARGEST_GROUP, GroupSizePreference.EITHER.maximumSize(LARGEST_GROUP));
    }

    @Test void everyChoiceReadsBackAsItself() {
        for (GroupSizePreference choice : GroupSizePreference.values()) {
            assertEquals(choice, GroupSizePreference.fromRange(choice.minimumSize(), choice.maximumSize(LARGEST_GROUP)));
        }
    }

    @Test void seededRangesReadAsTheClosestChoice() {
        assertEquals(GroupSizePreference.ONE_TO_ONE, GroupSizePreference.fromRange(2, 2));
        assertEquals(GroupSizePreference.EITHER, GroupSizePreference.fromRange(2, 3));
        assertEquals(GroupSizePreference.EITHER, GroupSizePreference.fromRange(2, 4));
        assertEquals(GroupSizePreference.EITHER, GroupSizePreference.fromRange(2, 5));
        assertEquals(GroupSizePreference.SMALL_GROUP, GroupSizePreference.fromRange(3, 4));
    }

    @Test void aProfileWithNoPreferenceYetReadsAsEither() {
        assertEquals(GroupSizePreference.EITHER, GroupSizePreference.fromRange(null, null));
    }
}
