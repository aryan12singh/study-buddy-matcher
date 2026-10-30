package com.studybuddy.matchrequest;

/**
 * States of a match request. Only a {@code PENDING} request can move, to {@code ACCEPTED} or
 * {@code DECLINED} by its receiver or to {@code CANCELLED} by its sender.
 */
public enum MatchRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED
}
