package com.studybuddy.admin;

/**
 * Account counts for the administrator dashboard: all accounts, active and deactivated, students
 * and administrators.
 */
public record AdminAccountsSummaryDto(
        long total,
        long active,
        long inactive,
        long students,
        long admins) {
}
