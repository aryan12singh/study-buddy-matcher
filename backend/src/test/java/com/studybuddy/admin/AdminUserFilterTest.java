package com.studybuddy.admin;

import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserFilterTest {

    private final User aliceUser = new User("alice@smu.edu.sg", "hash", Role.STUDENT);
    private final String alice = "Alice Tan";
    private final User admin = new User("admin@smu.edu.sg", "hash", Role.ADMIN);

    @Test
    void emptyFilterMatchesEveryone() {
        assertTrue(AdminUserFilter.none().matches(aliceUser, alice));
        assertTrue(AdminUserFilter.none().matches(admin, null));
    }

    @Test
    void roleFilterMatchesOnlyThatRole() {
        AdminUserFilter admins = new AdminUserFilter(Role.ADMIN, null, null);

        assertTrue(admins.matches(admin, null));
        assertFalse(admins.matches(aliceUser, alice));
    }

    @Test
    void activeFilterMatchesAccountStatus() {
        admin.setActive(false);

        assertTrue(new AdminUserFilter(null, false, null).matches(admin, null));
        assertFalse(new AdminUserFilter(null, true, null).matches(admin, null));
    }

    @Test
    void searchMatchesPartOfEmailOrNameIgnoringCase() {
        assertTrue(new AdminUserFilter(null, null, "TAN").matches(aliceUser, alice));
        assertTrue(new AdminUserFilter(null, null, "alice@").matches(aliceUser, alice));
        assertFalse(new AdminUserFilter(null, null, "bob").matches(aliceUser, alice));
    }

    @Test
    void searchOnNameNeverMatchesAnAdminWithoutProfile() {
        assertFalse(new AdminUserFilter(null, null, "tan").matches(admin, null));
    }

    @Test
    void blankSearchIsIgnored() {
        assertTrue(new AdminUserFilter(null, null, "  ").matches(admin, null));
    }
}
