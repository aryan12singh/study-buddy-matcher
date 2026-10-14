# Design decisions

The Team C decisions below were agreed on 7 October 2026. Sources: the IS442 brief, the `oop.pdf`
discussion and issues #10-14. The brief requires admin deletion without saying hard or soft;
issue #14 asks for separate Delete and Deactivate actions. These are our design choices,
not rules from the brief.

| Decision | Choice and reason | Alternative / affected work |
| --- | --- | --- |
| Leader model | Creating student + leader FK + accepted membership; STUDENT/ADMIN are the only roles | A subclass or GROUP_LEADER JWT role would misrepresent per-group permissions; #12 |
| Deactivate | Retain identity/profile/history, revoke access, end active buddies, cancel pending buddy requests, close led groups/reject applicants, reject own pending applications, leave open groups atomically; closed groups keep the leader and members | Merely flipping active leaves usable relations; #10/#12/#14 |
| Reactivate | Fresh sign-in, no relation restoration, old JWT versions remain invalid | Restoring ended resources or old JWTs changes consent; #14 |
| Permanent delete | Remove user/student and all dependencies; remove led groups and dependent records, preserve other groups, notify surviving participants generically and purge deleted-resource links | Delete-as-deactivate fails distinct operation; orphan leaders/implicit transfer unsupported; #14 |
| Administrative safeguards | Self removal blocked, and the actor must be an active admin, so one always remains; the exclusive lifecycle guard serializes admins removing each other and rejects revoked actors | Check-then-write count races; #14 |
| Role changes | Select at create; fixed on normal edit, no implicit Student/Admin conversion | Conversion needs separate profile/data policy; #14 |
| Email identity | Strip + lowercase; unique DB expression index; inactive email reserved, deletion releases it | Case-sensitive app-only check permits duplicates; #6/#14 |
| Contact reads | Separate public/connected profile DTOs; self or current buddy only, never group peers or admin payloads | Entity serialization/CSS hiding discloses private values; #11/#14 |
| Admin contact writes | Required on student creation; optional replacement on edit, omission preserves stored number | Prefilling a private contact in admin payload bypasses the agreed read policy; #14 |
| Usage | Last successful sign-in, unended connections, current accepted membership in open groups including leader; separately labelled history metrics | Online status and guessed scores are not usage evidence; #14 |
| Request context | PROFILE or MATCHING with optional known course/goal; retain and validate supplied context | Do not invent a search course from profile preferences; #10/#3 |
| Group capacity | Accepted members including leader count; pending requests do not. Group row serializes approval, edit, remove and close | Separate pre-counts allow over-capacity commits; #12 |
| Group closure | One-way closed record; pending applicants rejected, other accepted members notified; no new requests/approval/edit | Student leave, leader transfer, reopen and dated sessions are separate scope; #12 |
| Optional group data | Preserve existing acceptance of absent goals/mode/weekly slots; always require name/course/capacity >=2 | Inventing compulsory choices changes a graded field requirement; #12 |
| Weekly times | Whole minutes, same-day start before end, Asia/Singapore. Group schedules may be empty or overlap; a student's own schedule rejects same-day overlap | Group meeting slots and personal availability have distinct validation contracts; dated sessions and overnight splitting remain separate scope; #8/#12 |
| Event times | Java Instant + PostgreSQL timestamptz + Z API timestamps; legacy local values explicitly converted from Asia/Singapore | Relabelling timestamp without conversion shifts recorded moments; matching-owned timestamps untouched |
| Notifications | One service, safe summary/resource metadata, recipient scope, persistent read state and transaction/event-key deduplication | Email/SMS/push/reminders and session events deferred; #13 |
| Database access | Backend JDBC owner writes; browser table/sequence grants revoked, RLS enabled with no browser policies | Supabase Auth/RLS browser access is a different architecture from application JWTs; #11 |
| Cross-instance write order | Shared transaction advisory guard for domain writes; exclusive for account lifecycle/seed; sorted user rows before group/request rows; actor version rechecked | Process mutex alone fails multiple instances; exclusive all writes unnecessarily blocks independent work |
| Windows | Decorative title bars and conventional routes; shared retro components are the implemented baseline | Movable stacking manager adds no OO value. The existence of an implemented screen does not certify latest-Figma acceptance; #9/#41 |
| Data freshness | Refetch on mutations/focus/visibility plus bounded poll; purge sensitive view state on disconnect/logout/401; forms preserve inputs | A saved/displayed number cannot be erased from someone else's memory; #11/#13 |
| Seed | Explicit opt-in, runtime passwords, missing identities/courses only, no resets | Shared destructive reseeds and committed/default passwords unsafe; #5/#44 |
| Shared foundation | Minimum auth/client/shell/course/seed/counts for C to run independently, documented interfaces for B/A reuse | Duplicate full B product implementation would create integration conflict |

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

## Platform and own-profile decisions (Team B boundary)

This describes the implemented platform and the 13 October compulsory completion work.
Team ownership describes responsibility; it does not establish unrecorded individual authorship
or human acceptance.

| Decision | Choice and reason | Alternative / affected work |
| --- | --- | --- |
| Identity/profile model | `Student` composes `User` with a shared `@MapsId` identity; administrators need no student profile | User inheritance would mix account/security concerns with student-only preferences; #5/#6/#7 |
| Authentication | Existing Spring Security/JJWT/BCrypt; server validates role, active account and token version on each request | Browser route guards alone cannot enforce authorization; Supabase browser auth is a different model; #6 |
| Own profile boundary | `MyProfileAssembler` returns self-only profile/contact fields; other-student reads use the public/connected privacy assembler | Reusing the own-profile DTO for discovery would expose contact/email; #7/#11 |
| Saved buddy target | Independent of course enrolments; Home uses it for a matching preview. Find buddies requires a current course or goal | Claiming it always becomes the finder's default disagrees with current UI behavior; #7/#9 |
| Preference representation | Categorical mode/group-size controls; group size becomes the agreed inclusive min/max interval; optional goal set | Storing another competing preference representation complicates Team A integration; #7 |
| Whole-week replacement | Serialize per-student profile/availability writes before collection read/delete/insert; return a complete replacement | Individually valid concurrent weeks must never become a combined invalid week; #7/#8 |
| Recoverable owner-editor refresh | Keep the other unsaved profile/week draft mounted after a successful save followed by a transient read failure; preserve retry/discard controls. Logout, identity changes and lost authorization clear it | Relaxing privacy purge globally would retain other students' private responses; #6/#7/#8 |
| Time draft ownership | Shared editor reports incomplete/invalid raw text and dirty state to profile/group forms; save is blocked until valid, and exit protection includes that text | Keeping an old valid payload while showing a new invalid value silently saves the wrong input; #8/#12 |
| Overlap policy | Personal availability rejects overlapping same-day blocks; groups permit overlap and the shared editor explains the applicable policy | A generic overlap warning must not imply a group API rejection; #8/#12 |
| Setup and schema | Development packaging skips tests; the isolated harness provisions/migrates PostgreSQL for context/HTTP tests. Ordered SQL is required on runtime databases | Local application YAML does not provision the test profile; Hibernate update cannot replace custom migration/adoption/RLS work; #5/#17 |
| Responsive/font baseline | Shared decorative chrome, Tahoma/Verdana/system body fonts, original CSS pixels and wrapping narrow layouts | A pixel font/window manager is not needed to implement the current design. Latest-Figma, contrast/projector and full browser checks remain acceptance gates; #9/#41/#42 |

The whole-system boundary and platform use cases are in [diagrams/PLATFORM.md](diagrams/PLATFORM.md).

## Cross-team decisions still owned elsewhere

Team A retains group-size preference interpretation, scoring/thresholds/search and matching
configuration. Current shared fields store a numeric min/max interval; C displays that
existing representation without deciding its scoring semantics. The team has recorded the
learning-style interpretation in the baseline contract below. Team B retains the
own-profile/preferences/availability editor and dashboard. Team A's
matching preview is a narrow integration in that dashboard, described below; Team C does
not own those profile editors.

Global online status, group-score recommendations and richer notes/task boards remain proposals.
The selected E1 AI-explanation and E2 room/session/calendar work is still pending on merged
main; PR #52 is a separate draft foundation. Agenda displays the actual recurring schedule and
goals. The compulsory UI does not infer presence, progress or dated-session data. See the
[selected-extra map](REQUIREMENT_COVERAGE.md#selected-extras-and-delivery).

## Schedule and submission record

The formal brief states Week 13, a 12-minute demo and 8-minute Q&A. Team planning records
11 October for the compulsory core, 24 October for the internal feature freeze, 25 October
for extras/integration delivery, 8 November for materials/rehearsal, and 15 November 2026
23:59 SGT for final handoff. Internal freeze and delivery are distinct milestones.

These are team targets. The exact LMS deadline, final submitted version/receipt and private
peer-evaluation completion still need direct confirmation; neither the repository date nor
a local build/export proves submission. No LMS confirmation is claimed here.

## UI polish

| Choice | Behaviour and integration consequence |
| --- | --- |
| Existing APIs | Group forms, membership, privacy and admin flows use the current authenticated contracts; no new backend endpoint, schema, configuration or dependency was required |
| One data router | `routes.tsx` contains the unchanged guarded route paths; `main.tsx` creates the browser router once and `App` supplies auth and `RouterProvider`. This enables the supported router blocker without custom history interception |
| Transient drafts | Group/account edits and buddy/application dialogs prompt on dirty SPA exits, Cancel/X/Escape where applicable, and native reload. Successful saves leave normally; choosing to leave a pending write does not cancel its backend transaction. Late completion cannot redirect or invoke an abandoned dialog callback |
| Destination feedback | Generic create/update/delete messages are displayed at the destination, consumed from the history state and dismissible. Back/reload does not replay them; no account/contact/password/draft values enter these messages |
| URL views | Groups use `view`, `courseId`, `studyGoal`, `studyMode`; notifications use `view`. Tabs push history; group filter edits replace the current entry. Invalid enum/numeric values default safely and unrelated query keys are preserved |
| Weekly editing | Duplicate creates an independent block; copy adds it to selected other days without adding an identical existing block. The live preview sorts for display without reordering the input. Existing whole-minute, same-day validation remains authoritative |
| Schedule draft validation | Invalid/incomplete text participates in save blocking and unsaved-change protection; clearing/changing a time cannot silently submit an older valid slot. Own-profile overlap validation and group overlap permission remain distinct |
| Copy convenience | An explicit click copies the canonical group URL without query/hash, or an already-authorised contact. Denied/unavailable clipboard gives manual guidance. Contacts are never duplicated into the fallback or persisted by the component |
| Honest decoration | Original CSS pixel patterns and deterministic name initials carry no inferred identity/presence; capacity uses server member/max counts; loading remains indeterminate. No made-up percentage or online indicator |
| Responsive interaction | Admin data keeps table semantics with explicit row/cell roles when rendered as narrow cards; visible labels/actions, wrapping, keyboard focus and reduced motion are retained |
| Nested dialogs | Scroll remains locked until the last modal closes, even when a route exit removes dialogs out of order; each dialog retains its keyboard focus/Escape handling |

## Matching decisions (Team A)

Implementation choices for [#1](https://github.com/aryan12singh/study-buddy-matcher/issues/1),
[#2](https://github.com/aryan12singh/study-buddy-matcher/issues/2),
[#3](https://github.com/aryan12singh/study-buddy-matcher/issues/3) and
[#4](https://github.com/aryan12singh/study-buddy-matcher/issues/4), recorded 13 October 2026.
The brief's Appendix A permits each group to choose formulas and weights. These choices
complete the current issue scope; this entry does not assert professor or product acceptance.

| Question | Implemented choice and reason | Alternative / affected work |
| --- | --- | --- |
| Which criteria? | Five independent `CriterionScorer` implementations: course, availability, meeting mode, goals and group size; reuse a common 0-1 contract | Monolithic conditional scoring makes strategies harder to explain; #1 |
| Missing preferences | Enabled missing criterion scores 0.5 as neutral compatibility; explicit filters exclude unconfirmed preferences | Treating every missing field as a perfect match or incompatible silently rewards/punishes unfinished profiles; #1/#3 |
| Course in focus | Explicit search course, otherwise caller's saved target. Candidate target gives 1, enrolment-only gives 0.5, no alignment 0; no focus gives 0.5 | Saved target cannot silently replace the user's current course selection. Enrolment and buddy target remain separate concepts; #1/#2/#7 |
| Goals | Explicit search goal is the singleton focus; otherwise shared saved goals / caller goal count. Candidate must also satisfy a selected goal filter | Current search intent takes precedence over saved preferences; symmetric Jaccard answers a different coverage question; #1/#2 |
| Group preference | Categorical UI maps to inclusive numeric intervals (2-2, 3-maximum, 2-maximum); scorer/filter use overlap | Brief repeats the label "preferred study mode" for group size, while Appendix A gives intervals; this reconciles both examples; #1/#2/#7 |
| Availability | Merge per-day duplicate/overlapping/adjacent slots, then intersect unions. Shared unique minutes / caller unique minutes. Same-day half-open intervals | Pairwise summation alone double-counts duplicate legacy inputs; #1/#8 |
| Weekly timezone | Recurring schedules mean Asia/Singapore campus-local day/time; no conversion by browser timezone or DST | UTC is used for event timestamps, not weekly preference slots; #1/#8 |
| Disabled criteria | Global enable flags remove scoring and priority influence. Every strategy stores all five weights, disabled weights 0 and enabled sum 1; at least one enabled | Weight 0 alone cannot disable a lexicographic priority; #1/#4 |
| Strategy priority | Balanced uses total; enabled availability/course priority sorts first then total; disabling that priority falls back to total | Priority weights remain editable. At equal priority the weighted term is equal, so it cannot disturb the remaining-preference tie-break; #1/#4 |
| Stable ordering | Strategy order, case-insensitive name, then ID. Alternative sorts start with shared hours or caller-year equality, then reuse that chain | Database/insertion order and quality grouping are not deterministic ranking rules; #2/#3 |
| Search entry | Require a current course or goal. Additional filters combine with AND; personal filters/sorts remain usable when the corresponding scoring criterion is disabled and never mutate admin config | Returning all profiles for an unqualified search contradicts the current issue requirement; #2/#3 |
| Candidate eligibility | Exclude self, inactive/deleted accounts and active connections; retain pending candidates with incoming/outgoing state | Existing buddies belong on Connections; a pending request is still visible but cannot be resent; #2/#10 |
| Results bound | Default 20, maximum 50, configured in YAML. Count eligible filtered results before limiting, sort before truncation | Full pagination is unnecessary for the bounded demonstration dataset; #2 |
| Quality and threshold | Quality cut-offs remain YAML defaults; admin minimum total score and student minimum quality both apply | A user filter can narrow, never override a global threshold; #1/#2/#4 |
| Explainability | Immutable score value includes bounded total, criterion breakdown, unique shared hours, deterministic explanation and quality. DTOs add enabled metadata from the same settings snapshot, relevant course and public profile data | Percent compatibility is not a success probability; AI wording never determines ranking; #1/#2/#15 |
| Default/override precedence | External startup defaults apply until saved overrides exist. Explicit restore clears overrides and reuses current external defaults; overrides persist across restarts | Silent reset on each startup loses admin settings; #4 |
| Configuration version | Persisted monotonically increasing revision; expected revision required for save/restore, stale writes conflict. Search reads one repeatable snapshot and returns its version | Timestamp-only or memory-only versions cannot protect concurrent writes or future explanation caches across restart; #1/#2/#4/#15 |
| Home integration | Small matching preview reads the same search/configuration response; uses saved target or goal only to form a valid current request, with a clear first-use link when neither exists | Separate dashboard heuristics can disagree with Find buddies; changes to Team B Home are limited integration work; #2/#3/#9 |

## Baseline requirement contract

The following choices were accepted by the project owner for compulsory delivery on
14 October 2026. They settle the implementation contract for #7/#41; they do not assert a
professor response or grading acceptance.

### Contact-number format (#7/#41)

Accept 8 ASCII digits starting with 6, 8 or 9 for a local Singapore number, or `+` followed by
7–15 ASCII digits whose first digit is 1–9 for international input. Remove ASCII whitespace,
hyphens, parentheses and dots. Store local Singapore input as `+65` plus the digits; retain
the normalized leading `+` for international input. Reject letters, extensions, repeated or
misplaced `+`, other punctuation and out-of-bound lengths. This checks representation only;
country/carrier assignment and reachability are not verified.

`InputRules.contactNumber` governs registration/admin creation, own-profile replacement and
optional admin contact replacement. Frontend validation uses the same grammar and examples.
Admin omission preserves the stored contact and never exposes it in a response. Legacy
contacts are not rewritten automatically; replacements/full profile writes require the
accepted format. New synthetic seed records use accepted formats, while repeat seeding
continues to preserve existing profiles and passwords.

Keeping arbitrary nonblank text would leave #7's format validation incomplete. Limiting
all students to Singapore numbers would exclude international contacts; a phone-provider
lookup or new library is unnecessary for this scoped format contract.

### Undefined learning-style wording (#41/#7)

The objective names “preferred learning style”, but the functional field list and Appendix A
supply no separate taxonomy or scorer. The team interprets it through the defined study
preferences: meeting mode, study goals and group preference, alongside course and availability.
No additional field, guessed categories or sixth criterion is added.

This accepted team interpretation completes the baseline implementation scope for #1/#7/#41.
It is not presented as professor confirmation. Any later professor instruction requiring a
separate field is a coordinated A/B change request, with its categories, API, scoring and
validation agreed before implementation. Presentation and grading acceptance remain human gates.
