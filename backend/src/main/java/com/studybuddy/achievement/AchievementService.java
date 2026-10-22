package com.studybuddy.achievement;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroupRepository;
import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.StudySession;
import com.studybuddy.studysession.StudySessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives badges and the weekly study streak from data the student already has.
 * Nothing here is stored: every value is recomputed from connections, requests, groups and sessions.
 */
@Service
@Transactional(readOnly = true)
public class AchievementService {
    private final AccountAccess access;
    private final ConnectionRepository connections;
    private final MatchRequestRepository requests;
    private final GroupMembershipRepository memberships;
    private final StudyGroupRepository groups;
    private final StudySessionRepository sessions;
    private final AchievementProperties properties;
    private final Clock clock;
    private final BadgeEvaluator evaluator = new BadgeEvaluator();
    private final WeeklyStreakCalculator streaks = new WeeklyStreakCalculator();

    public AchievementService(AccountAccess access, ConnectionRepository connections, MatchRequestRepository requests,
            GroupMembershipRepository memberships, StudyGroupRepository groups, StudySessionRepository sessions,
            AchievementProperties properties, Clock clock) {
        this.access = access;
        this.connections = connections;
        this.requests = requests;
        this.memberships = memberships;
        this.groups = groups;
        this.sessions = sessions;
        this.properties = properties;
        this.clock = clock;
    }

    public AchievementsDto forStudent(Long actorId) {
        access.requireStudent(actorId);
        List<StudySession> mySessions = sessions.findMine(actorId);
        List<Instant> starts = mySessions.stream()
                .flatMap(session -> session.schedule().occurrences().stream())
                .map(SessionOccurrenceDto::startAt)
                .toList();
        LocalDate today = LocalDate.now(clock.withZone(properties.weekZone()));
        StreakDto streak = streaks.calculate(starts, properties.weekZone(), today);

        List<BadgeDto> badges = List.of(
                evaluator.evaluate(BadgeDefinition.CONNECTOR, connections.countActiveByStudentId(actorId), properties.connections()),
                evaluator.evaluate(BadgeDefinition.FIRST_MOVE, requests.countBySenderId(actorId), properties.requestsSent()),
                evaluator.evaluate(BadgeDefinition.TEAM_PLAYER, memberships.countJoinedByStudentId(actorId), properties.groupsJoined()),
                evaluator.evaluate(BadgeDefinition.FOUNDER, groups.countByLeaderId(actorId), properties.groupsLed()),
                evaluator.evaluate(BadgeDefinition.PLANNER, starts.size(), properties.sessionsPlanned()),
                evaluator.evaluate(BadgeDefinition.GROVE_KEEPER, streak.bestWeeks(), properties.streakWeeks()));

        int points = badges.stream().filter(BadgeDto::earned).mapToInt(badge -> badge.tier().points()).sum();
        int perLevel = properties.pointsPerLevel();
        int earned = (int) badges.stream().filter(BadgeDto::earned).count();
        return new AchievementsDto(points / perLevel + 1, points, perLevel - points % perLevel, perLevel,
                earned, badges.size(), streak, badges);
    }
}
