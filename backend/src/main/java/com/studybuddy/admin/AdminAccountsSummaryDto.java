package com.studybuddy.admin;

public record AdminAccountsSummaryDto(
        long total,
        long active,
        long inactive,
        long students,
        long admins) {
}
