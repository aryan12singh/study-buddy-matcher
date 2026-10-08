package com.studybuddy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseBoundaryTest extends PostgresHttpTest {
    @Test
    void anonymousAndAuthenticatedBrowserRolesCannotReadOrWriteApplicationTables() {
        student("private@example.test", "Private", "must-not-be-readable-by-browser-role");
        for (String role : new String[]{"anon", "authenticated"}) {
            for (String query : new String[]{"select contact_number from students", "select password_hash from users",
                    "select * from notifications", "insert into courses(code,name) values ('FORBIDDEN','Forbidden')"}) {
                String state = database.execute((ConnectionCallback<String>) connection -> {
                    try (var statement = connection.createStatement()) {
                        statement.execute("set role " + role);
                        try {
                            statement.execute(query);
                            return "unexpected-success";
                        } catch (SQLException denied) {
                            return denied.getSQLState();
                        } finally {
                            statement.execute("reset role");
                        }
                    }
                });
                assertEquals("42501", state, role + " must not bypass backend authorization");
            }
        }
        assertEquals(1, count("select count(*) from students"));
        assertEquals(16, count("select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace "
                + "where n.nspname='public' and c.relkind='r' and c.relrowsecurity"));
    }

    @Test
    void databaseUniquenessProtectsAgainstClientsThatSkipServiceChecks() {
        long a = student("alice@example.test", "Alice", "a"), b = student("bob@example.test", "Bob", "b");
        database.update("insert into match_requests(sender_id,receiver_id,status,created_at) values (?,?,'PENDING',now())", a, b);
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into match_requests(sender_id,receiver_id,status,created_at) values (?,?,'PENDING',now())", b, a));
        database.update("insert into connections(student_a_id,student_b_id,created_at) values (?,?,now())", a, b);
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into connections(student_a_id,student_b_id,created_at) values (?,?,now())", b, a));
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into users(email,password_hash,role,active,created_at) values ('ALICE@example.test','not-a-login','STUDENT',true,now())"));
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into connections(student_a_id,student_b_id,created_at) values (?,?,now())", a, a));
    }

    @Test
    void eventDeduplicationAndWholeMinuteSchedulesAreDatabaseEnforced() {
        long a = student("alice@example.test", "Alice", "a");
        database.update("insert into notifications(recipient_id,type,message,read,created_at,event_key) "
                + "values (?,'MATCH_REQUEST_RECEIVED','A request',false,now(),'one-event')", a);
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into notifications(recipient_id,type,message,read,created_at,event_key) "
                        + "values (?,'MATCH_REQUEST_RECEIVED','A request',false,now(),'one-event')", a));
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into availability_slots(student_id,day_of_week,start_time,end_time) values (?,'MONDAY','09:00:01','11:00')", a));
        assertThrows(DataIntegrityViolationException.class, () -> database.update(
                "insert into availability_slots(student_id,day_of_week,start_time,end_time) values (?,'MONDAY','11:00','09:00')", a));
    }
}
