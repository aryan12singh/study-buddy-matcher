# Known limitations and future work

Deliberate boundaries of the current build, each with the owner who would extend it and how the
design already allows it.

**Buddy requests and study groups (Team C)**

| Limitation | Current behaviour | Planned approach |
| --- | --- | --- |
| Leader transfer and reopening a group | A leader can only close a group, and closing is final | Handover and reopen transitions on `StudyGroup` |
| Re-request cooldown | A declined sender may ask again at once | A configurable cooldown checked by `MatchRequestService` |
| Group course changes and full groups | A leader can change the course after members join; applications to a full group stay pending | Lock the course once members join; reject or notify leftover applicants |

**Matching (Team A)**

| Enhancement | Why not now | How the design extends |
| --- | --- | --- |
| Preferred learning style | Named in the brief's objective, but no taxonomy is defined | One new `CriterionScorer`, one `MatchingCriterion` value and one weight; existing scorers are unchanged |
| Related courses (equivalent, corequisite) | The brief defines a course match as the same course | Admin-curated `CourseRelation(courseA, courseB, type)` that the course scorer and filter read |
| Completed courses and mentoring | Profiles record courses currently taken | A separate completed-courses list and an opt-in "can mentor" search, kept apart from peer matching |
| Required ("dealbreaker") criteria | Admins can only enable, disable and weight criteria | Each criterion becomes off, weighted or required; required ones act as filters |
| Admin ranking preview | Weight changes are visible only after saving | Run a sample search with the unsaved weights |

**Accounts and operations (Team B, admin parts Team C)**

| Enhancement | Notes |
| --- | --- |
| Change, forgot and admin-reset password; self-service deactivation or deletion | Reuse `User.revokeTokens()` so other sessions end, and the existing admin lifecycle services |
| Admin role changes and moderation of groups and requests | The brief limits the admin to accounts and matching |
| Course catalogue as a migration | A fresh install has no courses unless the opt-in demo seed runs |
| Shared state across several backend instances | Sign-in throttling, the AI allowance and the weather cache live in memory, which suits one instance |

**Notifications (Team C)**

| Enhancement | Notes |
| --- | --- |
| Real-time delivery | Pages poll about every 30 seconds; server-sent events or WebSocket later |
| Email, SMS and reminders | Deferred in [design decisions](DESIGN_DECISIONS.md) |
| Pagination | Notifications, groups and admin users return full lists, which suits the demo cohort |
