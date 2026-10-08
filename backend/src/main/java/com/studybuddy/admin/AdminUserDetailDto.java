package com.studybuddy.admin;

/**
 * One account as an admin sees it in detail: the summary fields plus usage.
 * {@code usage} is null for admin accounts, which take no part in matching
 * or groups.
 */
public record AdminUserDetailDto(
        AdminUserSummaryDto account,
        UserUsageDto usage,
        AdminStudentProfileDto profile) {
}
