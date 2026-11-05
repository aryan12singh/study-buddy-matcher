package com.studybuddy.integration;

import com.studybuddy.achievement.AchievementService;
import com.studybuddy.achievement.BadgeDto;
import com.studybuddy.achievement.BadgeTier;
import com.studybuddy.achievement.StreakDto;
import com.studybuddy.forest.ForestService;
import com.studybuddy.forest.TreeDto;
import com.studybuddy.forest.TreeSpecies;
import com.studybuddy.matching.MatchResultDto;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.seed.DemoSeedService;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the opt-in demo seed against migrated PostgreSQL and checks it gives the brief's minimum
 * data, varied preferences, the Appendix A example and a ready-to-demo presenter account.
 */
@TestPropertySource(properties = {
    "app.demo-seed.enabled=true",
    "app.demo-seed.student-password=Demo-student-1",
    "app.demo-seed.admin-password=Demo-admin-1"
})
class DemoSeedTest extends PostgresHttpTest {

    private static final String PRESENTER = "priya@demo.example.test";

    @Autowired
    private DemoSeedService seed;
    @Autowired
    private MatchService matches;
    @Autowired
    private AchievementService achievements;
    @Autowired
    private ForestService forest;

    @Test
    void seedsAtLeastTheBriefsMinimumCoursesAndStudents() {
        seed.seed();

        assertTrue(count("select count(*) from courses") >= 10);
        assertEquals(50, count("select count(*) from users where role='STUDENT'"));
        assertEquals(1, count("select count(*) from users where role='ADMIN'"));
        assertTrue(count("select count(distinct school) from students") >= 5, "students should span several schools");
        assertEquals(0, count("select count(*) from students where name like 'Demo Student%'"));
    }

    @Test
    void coversEveryStudyModeGoalAndGroupSizePreference() {
        seed.seed();

        for (StudyMode mode : StudyMode.values()) {
            assertTrue(count("select count(*) from students where preferred_study_mode=?", mode.name()) > 0, mode.name());
        }
        for (StudyGoal goal : StudyGoal.values()) {
            assertTrue(count("select count(*) from student_study_goals where study_goal=?", goal.name()) > 0, goal.name());
        }
        assertTrue(count("select count(*) from students where preferred_group_size_max = 2") > 0, "one-to-one");
        assertTrue(count("select count(*) from students where preferred_group_size_min > 2") > 0, "small group");
        assertTrue(count("select count(*) from students where preferred_group_size_min = 2 and preferred_group_size_max > 2") > 0, "either");
        assertTrue(count("select count(*) from students s where not exists (select 1 from availability_slots a where a.student_id=s.id)") >= 3,
            "a few students without a timetable show the empty state");
    }

    @Test
    void reproducesTheAppendixAExampleAsAStrongMatch() {
        seed.seed();
        long studentA = idOf("appendix.a@demo.example.test");
        long studentB = idOf("appendix.b@demo.example.test");
        long is442 = count("select id from courses where code='IS442'");

        MatchResultDto result = search(studentA, is442).stream()
            .filter(match -> match.studentId() == studentB).findFirst().orElseThrow();

        assertEquals(2.0, result.sharedHoursPerWeek(), 1e-9);
        assertEquals(0.85, result.totalScore(), 1e-9);
        assertEquals("STRONG", result.quality().name());
    }

    @Test
    void mostOfThePresentersMatchesShareTime() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long target = count("select target_course_id from students where id=?", presenter);

        List<MatchResultDto> results = search(presenter, target);

        assertTrue(results.size() >= 5, "presenter needs a meaningful list, got " + results.size());
        long sharing = results.stream().filter(match -> match.sharedHoursPerWeek() > 0).count();
        assertTrue(sharing * 2 > results.size(), sharing + " of " + results.size() + " matches share time");
    }

    @Test
    void preparesThePresentersRequestsConnectionsAndGroups() {
        seed.seed();
        long presenter = idOf(PRESENTER);

        assertEquals(1, count("select count(*) from match_requests where receiver_id=? and status='PENDING'", presenter));
        assertEquals(1, count("select count(*) from match_requests where sender_id=? and status='PENDING'", presenter));
        assertEquals(1, count("select count(*) from connections where (student_a_id=? or student_b_id=?) and ended_at is null", presenter, presenter));
        assertEquals(1, count("select count(*) from connections where (student_a_id=? or student_b_id=?) and ended_at is not null", presenter, presenter));
        long led = count("select id from study_groups where leader_id=? and active", presenter);
        assertEquals(3, count("select count(*) from group_memberships where study_group_id=?", led));
        assertEquals(2, count("select count(*) from group_join_requests where study_group_id=? and status='PENDING'", led));
        assertTrue(count("select count(*) from study_groups g where g.active and g.max_group_size = "
            + "(select count(*) from group_memberships m where m.study_group_id=g.id)") >= 1, "a full group");
        assertTrue(count("select count(*) from study_groups where not active") >= 1, "a closed group");
        assertTrue(count("select count(*) from users where role='STUDENT' and not active") >= 1, "a deactivated account");
        assertTrue(count("select count(*) from notifications where recipient_id=? and not read", presenter) > 0);
    }

    @Test
    void thePresentersHistoryShowsEveryRequestStatusAndAGroupTheyJoined() {
        seed.seed();
        long presenter = idOf(PRESENTER);

        assertEquals(1, count("select count(*) from match_requests where sender_id=? and status='DECLINED'", presenter));
        assertEquals(1, count("select count(*) from match_requests where sender_id=? and status='CANCELLED'", presenter));
        assertEquals(1, count("select count(*) from group_memberships m join study_groups g on g.id=m.study_group_id "
            + "where m.student_id=? and g.leader_id<>? and g.active", presenter, presenter), "a group the presenter joined");
    }

    @Test
    void thePresentersFeedHasOnlyTheOpenApplicationsUnread() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long led = count("select id from study_groups where leader_id=? and active", presenter);

        assertEquals(2, count("select count(*) from notifications where recipient_id=? and type='GROUP_JOIN_REQUEST_RECEIVED' "
            + "and resource_id=? and not read", presenter, led), "only the two pending applications");
        assertEquals(2, count("select count(*) from notifications where recipient_id=? and type='GROUP_JOIN_REQUEST_RECEIVED' "
            + "and resource_id=? and read", presenter, led), "the accepted members' requests were read");
        assertEquals(1, count("select count(*) from notifications where recipient_id=? and type='MATCH_REQUEST_RECEIVED' "
            + "and not read", presenter), "the pending buddy request");
    }

    @Test
    void preparesThePresentersGroupAndBuddySessions() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long led = count("select id from study_groups where leader_id=? and active", presenter);

        assertEquals(1, count("select count(*) from study_sessions where study_group_id=? and status='SCHEDULED' "
            + "and occurrence_count > 1 and start_at < now() - interval '2 weeks' "
            + "and start_at + (occurrence_count - 1) * interval '1 week' > now()", led), "a weekly series under way");
        assertEquals(1, count("select count(*) from study_sessions s join connections c on c.id=s.connection_id "
            + "where (c.student_a_id=? or c.student_b_id=?) and c.ended_at is null and s.start_at > now()", presenter, presenter));
    }

    @Test
    void preparesAClosedGroupQuizWithALeaderboardAndASubmittedPracticeQuiz() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long led = count("select id from study_groups where leader_id=? and active", presenter);
        long groupQuiz = count("select id from quizzes where group_id=?", led);

        assertEquals("CLOSED", database.queryForObject("select status from quizzes where id=?", String.class, groupQuiz));
        assertEquals(10, count("select count(*) from quiz_questions where quiz_id=?", groupQuiz));
        assertTrue(count("select count(*) from quiz_attempts where quiz_id=? and submitted_at is not null", groupQuiz) >= 3,
            "a leaderboard needs several members' scores");
        assertTrue(count("select count(distinct score) from quiz_attempts where quiz_id=?", groupQuiz) > 1,
            "members should not all tie");
        assertEquals(0, count("select count(*) from quiz_attempts where submitted_at - started_at < interval '1 minute'"),
            "every taker took a plausible time");
        assertTrue(count("select count(distinct submitted_at - started_at) from quiz_attempts where quiz_id=?", groupQuiz) > 1,
            "takers took different times");
        assertEquals(count("select id from study_sessions where study_group_id=?", led),
            count("select session_id from quizzes where id=?", groupQuiz), "the quiz belongs to the group's session");
        assertEquals(1, count("select count(*) from quizzes q join quiz_attempts a on a.quiz_id=q.id "
            + "where q.student_id=? and q.scope='PERSONAL' and a.submitted_at is not null", presenter));
    }

    @Test
    void thePresenterHasAnOpenPracticeQuizToTakeWithoutAnAiKey() {
        seed.seed();
        long presenter = idOf(PRESENTER);

        assertEquals(1, count("select count(*) from quizzes q where q.student_id=? and q.scope='PERSONAL' and q.status='OPEN' "
            + "and q.closes_at is null and not exists (select 1 from quiz_attempts a where a.quiz_id=q.id)", presenter));
    }

    @Test
    void preparesAStudyPlanWithSomeTopicsTickedOff() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long plan = count("select id from study_plans where student_id=?", presenter);

        long entries = count("select count(*) from study_plan_entries where plan_id=?", plan);
        long done = count("select count(*) from study_plan_entries where plan_id=? and completed", plan);
        assertTrue(entries > 3, "a roadmap of several weeks");
        assertTrue(done > 0 && done < entries, done + " of " + entries + " topics ticked off");
        assertEquals(20, count("select exam_date - first_block_date from study_plans where id=?", plan),
            "the exam is counted from the plan's own today");
    }

    @Test
    void preparesARoadmapForEveryUpcomingMeetingOfTheGroupSession() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long led = count("select id from study_groups where leader_id=? and active", presenter);
        long session = count("select id from study_sessions where study_group_id=?", led);
        long plan = count("select id from study_plans where session_id=? and scope='SESSION'", session);

        long meetings = count("select block_count from study_plans where id=?", plan);
        assertTrue(meetings >= 5, "the series' remaining meetings, got " + meetings);
        assertEquals(meetings, count("select count(distinct sequence) from study_plan_entries where plan_id=?", plan));
        assertEquals(120, count("select minutes_per_block from study_plans where id=?", plan));
        assertTrue(count("select count(*) from study_plans where id=? and first_block_date >= (now() at time zone 'Asia/Singapore')::date", plan) == 1,
            "the roadmap starts at the next meeting");
    }

    @Test
    void thePresenterOpensAchievementsWithBadgesAlreadyEarned() {
        seed.seed();

        List<BadgeDto> badges = achievements.forStudent(idOf(PRESENTER)).badges();

        assertTrue(badges.stream().filter(BadgeDto::earned).count() >= 4, "most badges earned");
        assertEquals(BadgeTier.SILVER, badges.stream().filter(badge -> badge.key().equals("PLANNER"))
            .findFirst().orElseThrow().tier(), "planner shows progress towards gold");
        for (String key : List.of("GROVE_KEEPER", "TEAM_PLAYER")) {
            assertTrue(badges.stream().filter(badge -> badge.key().equals(key)).findFirst().orElseThrow().earned(), key);
        }
    }

    @Test
    void thePresentersStreakRunsForSeveralWeeks() {
        seed.seed();

        StreakDto streak = achievements.forStudent(idOf(PRESENTER)).streak();

        assertTrue(streak.currentWeeks() >= 3, "current run " + streak.currentWeeks());
        assertTrue(streak.bestWeeks() >= streak.currentWeeks());
    }

    @Test
    void thePresentersForestShowsEveryTreeSpecies() {
        seed.seed();

        var trees = forest.forestOf(idOf(PRESENTER)).trees();

        assertEquals(EnumSet.allOf(TreeSpecies.class), trees.stream().map(TreeDto::species)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(TreeSpecies.class))), "one visit per tree tier");
        assertEquals(TreeSpecies.ANCIENT_BLOSSOM, trees.getFirst().species(), "newest visit first");
        assertTrue(forest.forestOf(idOf("jamie@demo.example.test")).trees().size() >= 3, "another student's forest");
    }

    @Test
    void seedingAgainLeavesExistingDataUntouched() {
        seed.seed();
        String[] tables = {"users", "courses", "match_requests", "connections", "study_groups",
            "group_memberships", "group_join_requests", "notifications", "availability_slots", "study_sessions",
            "quizzes", "quiz_attempts", "study_plans", "study_plan_entries", "focus_visits"};
        long[] before = Arrays.stream(tables).mapToLong(table -> count("select count(*) from " + table)).toArray();

        seed.seed();

        long[] after = Arrays.stream(tables).mapToLong(table -> count("select count(*) from " + table)).toArray();
        for (int index = 0; index < tables.length; index++) {
            assertEquals(before[index], after[index], tables[index]);
        }
    }

    @Test
    void thePresentersMatchesDifferEnoughToShowEachStrategy() {
        seed.seed();
        long presenter = idOf(PRESENTER);
        long is442 = count("select id from courses where code='IS442'");

        List<MatchResultDto> results = search(presenter, is442);

        // If every candidate scored the same on a criterion, changing its weight or switching
        // strategy would not visibly change the list in a demo.
        for (MatchingCriterion criterion : List.of(MatchingCriterion.COURSE, MatchingCriterion.AVAILABILITY,
                MatchingCriterion.STUDY_GOAL, MatchingCriterion.GROUP_SIZE)) {
            long distinct = results.stream().map(match -> scoreOf(match, criterion)).distinct().count();
            assertTrue(distinct > 1, criterion + " scores the same for every match");
        }
        // Availability-first should lift someone whom course-first would not.
        assertTrue(results.stream().anyMatch(match -> match.sharedHoursPerWeek() >= 3
                && scoreOf(match, MatchingCriterion.COURSE) < 1.0), "a match sharing lots of time but only taking the course");
    }

    private static double scoreOf(MatchResultDto match, MatchingCriterion criterion) {
        return match.breakdown().stream().filter(score -> score.criterion() == criterion).findFirst().orElseThrow().score();
    }

    private List<MatchResultDto> search(long searcher, long courseId) {
        return matches.search(new MatchSearchCriteria(courseId, null, null, null, null, null, null, 50), searcher).matches();
    }

    private long idOf(String email) {
        return count("select id from users where email=?", email);
    }
}
