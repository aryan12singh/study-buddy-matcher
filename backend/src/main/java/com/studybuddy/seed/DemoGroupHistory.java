package com.studybuddy.seed;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Dates a demo group's formation in the past. A group and its memberships record the moment they
 * are created and never change it, so a group replayed today would look brand new; the demo needs
 * one that has been meeting for weeks, so its earlier meetings count towards its members' streak.
 * Only the seed does this, and only on rows it has just created.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoGroupHistory {

    private final JdbcTemplate database;

    public DemoGroupHistory(JdbcTemplate database) {
        this.database = database;
    }

    /** Moves the group's creation and every current member's joining time to {@code formedAt}. */
    public void dateFormation(Long groupId, Instant formedAt) {
        Timestamp at = Timestamp.from(formedAt);
        database.update("update study_groups set created_at = ? where id = ?", at, groupId);
        database.update("update group_memberships set joined_at = ? where study_group_id = ?", at, groupId);
    }
}
