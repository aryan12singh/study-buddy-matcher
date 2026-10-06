# Design decisions

Team C decisions approved on 7 October 2026, implemented on the current feature branch.
Sources: formal IS442 brief, latest `oop.pdf` discussion, feature issues #10-14 and the
[approved plan](TEAM_C_IMPLEMENTATION_PLAN.md). The brief requires admin deletion but
does not prescribe hard versus soft semantics; the current issue #14 requires distinct
Delete and Deactivate actions. These are team design decisions, not invented professor rules.

| Decision | Choice and reason | Alternative / affected work |
| --- | --- | --- |
| Leader model | Creating student + leader FK + accepted membership; STUDENT/ADMIN are the only roles | A subclass or GROUP_LEADER JWT role would misrepresent per-group permissions; #12 |
| Deactivate | Retain identity/profile/history, revoke access, end active buddies, decline pending buddy requests, close led groups/reject applicants, reject own pending applications, remove memberships atomically | Merely flipping active leaves usable relations; #10/#12/#14 |
| Reactivate | Fresh sign-in, no relation restoration, old JWT versions remain invalid | Restoring ended resources or old JWTs changes consent; #14 |
| Permanent delete | Remove user/student and all dependencies; remove led groups and dependent records, preserve other groups, notify surviving participants generically and purge deleted-resource links | Delete-as-deactivate fails distinct operation; orphan leaders/implicit transfer unsupported; #14 |
| Administrative safeguards | Self removal blocked; exclusive lifecycle guard plus locked recount protects last usable admin and revoked actors | Check-then-write count races; #14 |
| Role changes | Select at create; fixed on normal edit, no implicit Student/Admin conversion | Conversion needs separate profile/data policy; #14 |
| Email identity | Strip + lowercase; unique DB expression index; inactive email reserved, deletion releases it | Case-sensitive app-only check permits duplicates; #6/#14 |
| Contact reads | Separate public/connected profile DTOs; self or current buddy only, never group peers or admin payloads | Entity serialization/CSS hiding discloses private values; #11/#14 |
| Admin contact writes | Required on student creation; optional replacement on edit, omission preserves stored number | Prefilling a private contact in admin payload bypasses the agreed read policy; #14 |
| Usage | Last successful sign-in, unended connections, current accepted membership in open groups including leader; separately labelled history metrics | Online status and guessed scores are not usage evidence; #14 |
| Request context | PROFILE or MATCHING with optional known course/goal; retain and validate supplied context | Do not invent a search course from profile preferences; #10/#3 |
| Group capacity | Accepted members including leader count; pending requests do not. Group row serializes approval, edit, remove and close | Separate pre-counts allow over-capacity commits; #12 |
| Group closure | One-way closed record; pending applicants rejected, other accepted members notified; no new requests/approval/edit | Student leave, leader transfer, reopen and dated sessions are separate scope; #12 |
| Optional group data | Preserve existing acceptance of absent goals/mode/weekly slots; always require name/course/capacity >=2 | Inventing compulsory choices changes a graded field requirement; #12 |
| Weekly times | Whole minutes, same-day start before end, Asia/Singapore; empty and overlapping slots allowed | Dated sessions, overnight splitting and shared-hour maths belong elsewhere; #8/#12 |
| Event times | Java Instant + PostgreSQL timestamptz + Z API timestamps; legacy local values explicitly converted from Asia/Singapore | Relabelling timestamp without conversion shifts recorded moments; matching-owned timestamps untouched |
| Notifications | One service, safe summary/resource metadata, recipient scope, persistent read state and transaction/event-key deduplication | Email/SMS/push/reminders and session events deferred; #13 |
| Database access | Backend JDBC owner writes; browser table/sequence grants revoked, RLS enabled with no browser policies | Supabase Auth/RLS browser access is a different architecture from application JWTs; #11 |
| Cross-instance write order | Shared transaction advisory guard for domain writes; exclusive for account lifecycle/seed; sorted user rows before group/request rows; actor version rechecked | Process mutex alone fails multiple instances; exclusive all writes unnecessarily blocks independent work |
| Windows | Decorative title bars and conventional routes; approved missing C layouts | Movable stacking manager adds no OO value; #9/#41 |
| Data freshness | Refetch on mutations/focus/visibility plus bounded poll; purge sensitive view state on disconnect/logout/401; forms preserve inputs | A saved/displayed number cannot be erased from someone else's memory; #11/#13 |
| Seed | Explicit opt-in, runtime passwords, missing identities/courses only, no resets | Shared destructive reseeds and committed/default passwords unsafe; #5/#44 |
| Foundation exception | Minimum auth/client/shell/course/seed/counts for C to run independently, documented interfaces for B/A reuse | Duplicate full B product implementation would create integration conflict |

## Transaction and database guarantees

All domain writes acquire shared advisory transaction lock `4422026`. Account lifecycle
and seed acquire it exclusively before reading affected data. User rows are locked in ID
order; then the group/request row is locked and state/counts are rechecked. Any actor
revoked during waiting fails authorization after locking. Group approval and capacity
edit take the same group lock. Partial unique indexes protect unordered pending buddy
pairs, active connection pairs, pending group/applicant pairs and recipient/event identities.

Application exceptions are mapped to safe RFC 9457 problems; SQL details remain internal.
Notifications commit in the same transaction as the transition. Real HTTP tests inject a
notification failure and verify rollback, and run simultaneous decisions against PostgreSQL.
The advisory protocol is for this backend's writes; arbitrary privileged SQL outside the
protocol can still break cross-row capacity rules. Operators must not bypass it.

## Cross-team decisions still owned elsewhere

Team A retains group-size preference interpretation, scoring/thresholds/search and matching
configuration. Current shared fields store a numeric min/max interval; C displays that
existing representation without deciding its scoring semantics. The objective mentions
learning style without a defined field/scorer: the professor clarification remains a shared
#41 item, rather than a new C field. Team B retains the full own-profile/preferences/
availability editor and dashboard. Their absence is not a remaining C screen.

Global online status, group-score recommendations, richer notes/task boards and dated/live
room features remain proposals/later scope. Agenda displays the actual recurring schedule
and goals. No fake presence, progress, room participants or scheduled session data is shown.

## Schedule and submission record

The formal brief states Week 13, a 12-minute demo and 8-minute Q&A. Current project issues
#41/#46 record 11 October core, 25 October extras/integration, 8 November materials/rehearsal,
and 15 November 2026 11:59 pm SGT final handoff. README and AGENTS use these team dates.
The presentation, human review, library/asset provenance, slides, full product integration
and private peer evaluation remain team delivery work; they are not claimed completed here.
