package com.studybuddy.forest;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.studyroom.RoomSettings;
import com.studybuddy.studyroom.RoomVisitEnded;
import com.studybuddy.studyroom.RoomVisitTicked;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grows one tree per study room visit. Each room tick credits the focus time since the student's
 * previous tick; a gap longer than a room lease means the student had gone, so the old visit is
 * finished and a new one starts. Planted trees are read back as a forest.
 */
@Service
@Transactional
public class ForestService {
    private final FocusVisitRepository visits;
    private final ForestAssembler assembler;
    private final TreeGrowth growth;
    private final RoomSettings rooms;
    private final AccountAccess accounts;
    private final TreeLottery lottery;
    private final Clock clock;

    public ForestService(FocusVisitRepository visits, ForestAssembler assembler, TreeGrowth growth,
        RoomSettings rooms, AccountAccess accounts, TreeLottery lottery, @Qualifier("studyRoomClock") Clock clock) {
        this.visits = visits;
        this.assembler = assembler;
        this.growth = growth;
        this.rooms = rooms;
        this.accounts = accounts;
        this.lottery = lottery;
        this.clock = clock;
    }

    public void recordTick(RoomVisitTicked tick) {
        // A tick from before the student left must not reopen the visit their leaving just ended.
        if (visits.existsByStudentIdAndStudyGroupIdAndEndedAtGreaterThan(tick.studentId(), tick.groupId(), tick.at())) return;
        var open = visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(tick.studentId(), tick.groupId());
        if (open.isPresent() && open.get().isOver(tick.at(), rooms.leaseLifetime())) {
            visits.end(open.get().getId(), open.get().getLastSeenAt());
            open = Optional.empty();
        }
        if (open.isEmpty()) {
            TreeDraw draw = lottery.draw();
            visits.open(tick.studentId(), tick.groupId(), tick.groupName(), tick.at(), draw.variant(), draw.plot());
            return;
        }
        FocusVisit visit = open.get();
        if (!tick.at().isAfter(visit.getLastSeenAt())) return;
        long millis = FocusCredit.focusedBetween(tick.timer(), visit.getLastSeenAt(), tick.at()).toMillis();
        visits.credit(visit.getId(), visit.getLastSeenAt(), tick.at(), millis);
    }

    public void endVisit(RoomVisitEnded ended) {
        visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(ended.studentId(), ended.groupId())
            .ifPresent(visit -> visits.end(visit.getId(), ended.at()));
    }

    /** Records a visit that already happened; used to seed the demo forest. */
    public void recordFinishedVisit(Long studentId, Long groupId, String groupName, Instant startedAt, Duration focused) {
        TreeDraw draw = lottery.draw();
        visits.insertFinished(studentId, groupId, groupName, startedAt, startedAt.plus(focused), focused.toMillis(),
            draw.variant(), draw.plot());
    }

    @Transactional(readOnly = true)
    public ForestDto forestOf(Long studentId) {
        accounts.requireStudent(studentId);
        return assembler.toForest(planted(studentId));
    }

    /** Trees planted so far; a visit still in progress does not count until it ends. */
    @Transactional(readOnly = true)
    public long plantedTreeCount(Long studentId) {
        return planted(studentId).size();
    }

    private List<FocusVisit> planted(Long studentId) {
        Instant now = clock.instant();
        return visits.findByStudentIdAndFocusedMillisGreaterThanEqualOrderByStartedAtDesc(studentId,
                growth.minimumMinutes() * 60_000L).stream()
            .filter(visit -> visit.isOver(now, rooms.leaseLifetime())).toList();
    }

    @Transactional(readOnly = true)
    public GrowingTreeDto growingIn(Long studentId, Long groupId) {
        accounts.requireStudent(studentId);
        Instant now = clock.instant();
        return visits.findByStudentIdAndStudyGroupIdAndEndedAtIsNull(studentId, groupId)
            .filter(visit -> !visit.isOver(now, rooms.leaseLifetime()))
            .map(assembler::toGrowing)
            .orElseGet(assembler::notVisiting);
    }
}
