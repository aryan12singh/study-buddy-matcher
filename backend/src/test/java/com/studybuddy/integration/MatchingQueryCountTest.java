package com.studybuddy.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cost of one match search. Candidates and their courses, goals and timetables
 * load in a fixed number of queries, so the statement count must not grow with the number
 * of students, and inactive students must never be loaded at all.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MatchingQueryCountTest extends PostgresHttpTest {

    private static final int SMALL_POOL = 10;
    private static final int LARGE_POOL = 40;
    private static final int INACTIVE_STUDENTS = 20;
    /**
     * Generous ceiling over the measured 11 Hibernate-prepared statements, so unrelated query
     * changes do not break the build. PostgreSQL's own statement log also counts transaction
     * control, which is why a logged demo-seed search shows about 20.
     */
    private static final long MAX_STATEMENTS_PER_SEARCH = 15;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private int candidates;

    private void addCandidatesUpTo(int total) {
        for (; candidates < total; candidates++) {
            student("candidate" + candidates + "@example.test", "Candidate " + candidates, "8222" + String.format("%04d", candidates));
        }
    }

    /** Runs one warm search, then measures a second identical one so first-use setup is not counted. */
    private Statistics measureOneSearch(String token, int expectedMatches) {
        String path = "/matches?courseId=" + course + "&limit=50";
        expect(call("GET", path, token, null), 200);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var matches = expect(call("GET", path, token, null), 200).path("matches");
        assertEquals(expectedMatches, matches.size());
        return statistics;
    }

    @Test
    void statementCountDoesNotGrowWithTheNumberOfCandidates() {
        student("searcher@example.test", "Searcher", "81110000");
        String token = login("searcher@example.test");

        addCandidatesUpTo(SMALL_POOL);
        long small = measureOneSearch(token, SMALL_POOL).getPrepareStatementCount();
        addCandidatesUpTo(LARGE_POOL);
        long large = measureOneSearch(token, LARGE_POOL).getPrepareStatementCount();

        assertEquals(small, large, "Searching " + LARGE_POOL + " candidates issued more statements than " + SMALL_POOL);
        assertTrue(large <= MAX_STATEMENTS_PER_SEARCH, "One search issued " + large + " SQL statements");
    }

    @Test
    void inactiveStudentsAreNeverLoaded() {
        student("searcher@example.test", "Searcher", "81110000");
        String token = login("searcher@example.test");
        addCandidatesUpTo(SMALL_POOL);
        long before = measureOneSearch(token, SMALL_POOL).getEntityLoadCount();

        for (int index = 0; index < INACTIVE_STUDENTS; index++) {
            long id = student("inactive" + index + "@example.test", "Inactive " + index, "8333" + String.format("%04d", index));
            database.update("update users set active=false where id=?", id);
        }
        long after = measureOneSearch(token, SMALL_POOL).getEntityLoadCount();

        assertEquals(before, after, "Inactive students or their timetables were loaded by the search");
    }
}
