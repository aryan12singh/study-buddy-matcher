package com.studybuddy.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;
import com.studybuddy.achievement.AchievementSessionRepository.PlannedSchedule;
import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AchievementServiceTest {
    private static final Long STUDENT = 7L;
    private final AccountAccess access = mock(AccountAccess.class);
    private final ConnectionRepository connections = mock(ConnectionRepository.class);
    private final MatchRequestRepository requests = mock(MatchRequestRepository.class);
    private final GroupMembershipRepository memberships = mock(GroupMembershipRepository.class);
    private final StudyGroupRepository groups = mock(StudyGroupRepository.class);
    private final AchievementSessionRepository sessions = mock(AchievementSessionRepository.class);
    private final AchievementProperties properties = new AchievementProperties(ZoneId.of("Asia/Singapore"), 3,
            new TierThresholds(1, 3, 5), new TierThresholds(1, 5, 10), new TierThresholds(1, 2, 3),
            new TierThresholds(1, 2, 3), new TierThresholds(1, 5, 10), new TierThresholds(2, 4, 8));
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T04:00:00Z"), ZoneOffset.UTC);
    private final AchievementService service = new AchievementService(access, connections, requests, memberships,
            groups, sessions, properties, new AchievementAssembler(properties), clock);

    private static PlannedSchedule weeklySession(String startLocal, int weeks) {
        PlannedSchedule session = mock(PlannedSchedule.class);
        LocalDateTime start = LocalDateTime.parse(startLocal);
        when(session.getStartLocal()).thenReturn(start);
        when(session.getEndLocal()).thenReturn(start.plusHours(1));
        when(session.getTimeZone()).thenReturn("Asia/Singapore");
        when(session.getOccurrenceCount()).thenReturn(weeks);
        when(session.getParticipationStartedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        return session;
    }

    @Test
    void derivesBadgesStreakAndLevelFromExistingRecords() {
        when(connections.countActiveByStudentId(STUDENT)).thenReturn(3L);
        when(requests.countBySenderId(STUDENT)).thenReturn(0L);
        when(memberships.countJoinedOpenByStudentId(STUDENT)).thenReturn(1L);
        when(groups.countByLeaderId(STUDENT)).thenReturn(0L);
        // Three weekly occurrences ending this week, giving a three-week streak.
        PlannedSchedule schedule = weeklySession("2026-10-06T10:00", 3);
        when(sessions.findEligibleSchedules(STUDENT)).thenReturn(List.of(schedule));

        AchievementsDto result = service.forStudent(STUDENT);

        assertThat(result.streak().currentWeeks()).isEqualTo(3);
        assertThat(result.streak().bestWeeks()).isEqualTo(3);
        assertThat(result.badges()).extracting(BadgeDto::key).containsExactly(
                "CONNECTOR", "FIRST_MOVE", "TEAM_PLAYER", "FOUNDER", "PLANNER", "GROVE_KEEPER");
        assertThat(result.badges()).extracting(BadgeDto::tier).containsExactly(
                BadgeTier.SILVER, null, BadgeTier.BRONZE, null, BadgeTier.BRONZE, BadgeTier.BRONZE);
        // silver 2 + bronze 1 + bronze 1 + bronze 1 = 5 points, 3 per level.
        assertThat(result.points()).isEqualTo(5);
        assertThat(result.level()).isEqualTo(2);
        assertThat(result.pointsToNextLevel()).isEqualTo(1);
        assertThat(result.earnedCount()).isEqualTo(4);
        assertThat(result.totalCount()).isEqualTo(6);
    }

    @Test
    void newStudentHasEveryBadgeLocked() {
        when(sessions.findEligibleSchedules(STUDENT)).thenReturn(List.of());

        AchievementsDto result = service.forStudent(STUDENT);

        assertThat(result.badges()).noneMatch(BadgeDto::earned);
        assertThat(result.level()).isEqualTo(1);
        assertThat(result.streak().currentWeeks()).isZero();
    }

    @Test
    void excludesOccurrencesBeforeCurrentParticipationButKeepsLaterDates() {
        PlannedSchedule schedule = weeklySession("2026-10-06T10:00", 4);
        // Starts exactly at the second occurrence: the first week is not this student's planning history.
        when(schedule.getParticipationStartedAt()).thenReturn(Instant.parse("2026-10-13T02:00:00Z"));
        when(sessions.findEligibleSchedules(STUDENT)).thenReturn(List.of(schedule));

        AchievementsDto result = service.forStudent(STUDENT);

        assertThat(result.badges().get(4).current()).isEqualTo(3);
        assertThat(result.streak().currentWeeks()).isEqualTo(2);
        assertThat(result.streak().bestWeeks()).isEqualTo(2);
        assertThat(result.streak().weekZone()).isEqualTo("Asia/Singapore");
    }

    @Test
    void recomputesTiersAndPointsWhenConnectionsAndSchedulesDisappear() {
        when(connections.countActiveByStudentId(STUDENT)).thenReturn(5L, 0L);
        when(requests.countBySenderId(STUDENT)).thenReturn(1L);
        when(groups.countByLeaderId(STUDENT)).thenReturn(1L);
        PlannedSchedule schedule = weeklySession("2026-10-06T10:00", 3);
        when(sessions.findEligibleSchedules(STUDENT)).thenReturn(List.of(schedule), List.of());

        AchievementsDto before = service.forStudent(STUDENT);
        AchievementsDto after = service.forStudent(STUDENT);

        assertThat(before.badges().getFirst().tier()).isEqualTo(BadgeTier.GOLD);
        assertThat(after.badges().getFirst().tier()).isNull();
        assertThat(after.streak().bestWeeks()).isZero();
        assertThat(after.points()).isLessThan(before.points());
        assertThat(after.badges().get(1).tier()).isEqualTo(BadgeTier.BRONZE);
        assertThat(after.badges().get(3).tier()).isEqualTo(BadgeTier.BRONZE);
    }

    @Test
    void nonStudentIsRejectedBeforeAnyLookup() {
        when(access.requireStudent(STUDENT)).thenThrow(new ForbiddenActionException("A student account is required"));

        assertThatThrownBy(() -> service.forStudent(STUDENT)).isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(sessions, connections, requests, memberships, groups);
    }
}
