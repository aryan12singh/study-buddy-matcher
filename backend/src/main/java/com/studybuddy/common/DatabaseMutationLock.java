package com.studybuddy.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Database transaction lock shared by every write. Account cleanup takes the
 * exclusive variant before reading accounts, so no relationship can be added
 * after cleanup has inspected it. PostgreSQL releases these locks on rollback
 * and commit; separate application instances follow the same protocol.
 */
@Component
public class DatabaseMutationLock {
    private static final long ACCOUNT_LIFECYCLE_KEY = 4422026L;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMutationLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void shared() {
        acquire("pg_advisory_xact_lock_shared");
    }

    public void exclusive() {
        acquire("pg_advisory_xact_lock");
    }

    private void acquire(String function) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A database mutation must run in a transaction");
        }
        jdbcTemplate.execute("select " + function + "(" + ACCOUNT_LIFECYCLE_KEY + ")");
    }
}
