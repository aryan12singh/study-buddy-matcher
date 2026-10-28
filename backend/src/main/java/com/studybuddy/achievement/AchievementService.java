package com.studybuddy.achievement;

import com.studybuddy.achievement.AchievementProperties.TierThresholds;
import com.studybuddy.common.AccountAccess;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroupRepository;
import com.studybuddy.studysession.SessionOccurrenceDto;
import com.studybuddy.studysession.SessionSchedule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives badge progress and a planned-session weekly streak from existing records. Counts are
 * recomputed on every request; only the highest tier each badge reaches is saved, so earned tiers
 * and the level never drop when a count falls.
 */
@Service
@Transactional
public class AchievementService {
    private final AccountAccess access;
    private final ConnectionRepository connections;
    private final MatchRequestRepository requests;
    private final GroupMembershipRepository memberships;
    private final StudyGroupRepository groups;
    private final AchievementSessionRepository sessions;
    private final AchievementProperties properties;
    private final EarnedBadgeRecorder recorder;
    private final AchievementAssembler assembler;
    private final Clock clock;
    private final BadgeEvaluator evaluator = new BadgeEvaluator();
    private final WeeklyStreakCalculator streaks = new WeeklyStreakCalculator();

    public AchievementService(AccountAccess access, ConnectionRepository connections, MatchRequestRepository requests,
            GroupMembershipRepository memberships, StudyGroupRepository groups, AchievementSessionRepository sessions,
            AchievementProperties properties, EarnedBadgeRecorder recorder, AchievementAssembler assembler, Clock clock) {
        this.access = access;
        this.connections = connections;
        this.requests = requests;
        this.memberships = memberships;
        this.groups = groups;
        this.sessions = sessions;
        this.properties = properties;
        this.recorder = recorder;
        this.assembler = assembler;
        this.clock = clock;
    }

    public AchievementsDto forStudent(Long actorId) {
        access.requireStudent(actorId);
        List<Instant> starts = sessions.findEligibleSchedules(actorId).stream()
                .flatMap(session -> new SessionSchedule(session.getStartLocal(), session.getEndLocal(),
                        session.getTimeZone(), session.getOccurrenceCount()).occurrences().stream()
                        .map(SessionOccurrenceDto::startAt)
                        .filter(start -> !start.isBefore(session.getParticipationStartedAt())))
                .toList();
        LocalDate today = LocalDate.now(clock.withZone(properties.weekZone()));
        StreakDto streak = streaks.calculate(starts, properties.weekZone(), today);

        List<BadgeProgress> progress = List.of(
                new BadgeProgress(BadgeDefinition.CONNECTOR, connections.countActiveByStudentId(actorId), properties.connections()),
                new BadgeProgress(BadgeDefinition.FIRST_MOVE, requests.countBySenderId(actorId), properties.requestsSent()),
                new BadgeProgress(BadgeDefinition.TEAM_PLAYER, memberships.countJoinedOpenByStudentId(actorId), properties.groupsJoined()),
                new BadgeProgress(BadgeDefinition.FOUNDER, groups.countByLeaderId(actorId), properties.groupsLed()),
                new BadgeProgress(BadgeDefinition.PLANNER, starts.size(), properties.sessionsPlanned()),
                new BadgeProgress(BadgeDefinition.GROVE_KEEPER, streak.bestWeeks(), properties.streakWeeks()));

        Map<BadgeDefinition, BadgeTier> liveTiers = new EnumMap<>(BadgeDefinition.class);
        progress.forEach(badge -> liveTiers.put(badge.definition(), evaluator.tierFor(badge.count(), badge.thresholds())));
        Map<BadgeDefinition, BadgeTier> earnedTiers = recorder.keepHighest(actorId, liveTiers);
        List<BadgeDto> badges = progress.stream()
                .map(badge -> evaluator.evaluate(badge.definition(), badge.count(), badge.thresholds(),
                        earnedTiers.get(badge.definition())))
                .toList();

        return assembler.toDto(badges, streak);
    }

    /** One badge's live count and the thresholds it is measured against. */
    private record BadgeProgress(BadgeDefinition definition, long count, TierThresholds thresholds) {
    }
}
