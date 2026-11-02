package com.studybuddy.forest;

import com.studybuddy.studyroom.RoomVisitEnded;
import com.studybuddy.studyroom.RoomVisitTicked;
import com.studybuddy.studyroom.StudentRoomAccessRevoked;
import com.studybuddy.studyroom.StudyRoomClosed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Feeds room activity to the forest. Trees are a reward, not part of being in a room: after the room
 * change commits, each update is handed to the forest worker and runs in its own transaction there,
 * and a failure is logged rather than failing the student's poll.
 */
@Component
public class ForestVisitListener {
    private static final Logger LOG = LoggerFactory.getLogger(ForestVisitListener.class);
    private final ForestService forest;
    private final TaskExecutor updates;
    private final TransactionTemplate transaction;

    public ForestVisitListener(ForestService forest, @Qualifier("forestUpdates") TaskExecutor updates,
        PlatformTransactionManager transactions) {
        this.forest = forest;
        this.updates = updates;
        this.transaction = new TransactionTemplate(transactions);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTick(RoomVisitTicked tick) {
        update(() -> forest.recordTick(tick), "grow the tree for student " + tick.studentId() + " in group " + tick.groupId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnded(RoomVisitEnded ended) {
        update(() -> forest.endVisit(ended), "plant the tree for student " + ended.studentId() + " in group " + ended.groupId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRevoked(StudentRoomAccessRevoked revoked) {
        update(() -> forest.endVisitsOf(revoked), "end the visits of student " + revoked.studentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClosed(StudyRoomClosed closed) {
        update(() -> forest.endVisitsIn(closed), "end the visits to group " + closed.groupId());
    }

    private void update(Runnable change, String description) {
        updates.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> change.run());
            } catch (RuntimeException failure) {
                LOG.warn("Could not {}", description, failure);
            }
        });
    }
}
