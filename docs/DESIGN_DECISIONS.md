# Design decisions

The Team C decisions below were agreed on 7 October 2026. Sources: the IS442 brief, the `oop.pdf`
discussion and issues #10-14. The brief requires admin deletion without saying hard or soft;
issue #14 asks for separate Delete and Deactivate actions. These are our design choices,
not rules from the brief.

| Decision | Choice and reason | Alternative / affected work |
| --- | --- | --- |
| Leader model | Creating student + leader FK + accepted membership; STUDENT/ADMIN are the only roles | A subclass or GROUP_LEADER JWT role would misrepresent per-group permissions; #12 |
| Deactivate | Retain identity/profile/history, revoke access, end active buddies, cancel pending buddy requests, close led groups/reject applicants, withdraw own pending applications, leave open groups atomically; closed groups keep the leader and members | Merely flipping active leaves usable relations; #10/#12/#14 |
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
| Group closure | One-way closed record; pending applicants rejected, other accepted members notified; no new requests/approval/edit. E2 also revokes room leases and cancels scheduled sessions | Leader transfer and reopen remain separate proposals; #12/#16 |
| Taking back an action | Sender withdraws a pending buddy request (`CANCELLED`); applicant withdraws a pending application (`WITHDRAWN`); member leaves an open group, which also ends their study-room presence. Each confirms first and notifies the other party. The leader cannot leave and closes the group instead | Without these a pending request blocked a pair indefinitely, and deactivation had to record withdrawals as leader rejections |
| Optional group data | Preserve existing acceptance of absent goals/mode/weekly slots; always require name/course/capacity >=2 | Inventing compulsory choices changes a graded field requirement; #12 |
| Weekly times | Whole minutes, same-day start before end, Asia/Singapore. Group schedules may be empty or overlap; a student's own schedule rejects same-day overlap | Group meeting slots and personal availability have distinct validation contracts; E2 dated sessions use their own service; automatic overnight splitting remains outside the weekly contract; #8/#12/#16 |
| Event times | Java Instant + PostgreSQL timestamptz + Z API timestamps; legacy local values explicitly converted from Asia/Singapore | Relabelling timestamp without conversion shifts recorded moments; matching-owned timestamps untouched |
| Notifications | One service, safe summary/resource metadata, recipient scope, persistent read state and transaction/event-key deduplication; E2 adds actual session change events | Email/SMS/push and scheduled reminder delivery remain separate proposals; #13/#16 |
| Database access | Backend JDBC owner writes; browser table/sequence grants revoked, RLS enabled with no browser policies | Supabase Auth/RLS browser access is a different architecture from application JWTs; #11 |
| Cross-instance write order | Shared transaction advisory guard for relationship/profile/room/session transitions; exclusive for account lifecycle/seed; sorted user rows before parent/request rows; actor version rechecked. Room reads and existing-lease renewals avoid the group lock | Process mutex alone fails multiple instances; serializing every poll unnecessarily blocks independent work |
| Windows | Decorative title bars and conventional routes; shared retro components are the implemented baseline | Movable stacking manager adds no OO value. The existence of an implemented screen does not certify latest-Figma acceptance; #9/#41 |
| Data freshness | Refetch on mutations/focus/visibility plus bounded poll; a background refresh keeps the last response visible until replaced and clears it if the refresh fails; purge sensitive view state on disconnect/logout/401; forms preserve inputs | A saved/displayed number cannot be erased from someone else's memory; #11/#13 |
| Sign-in throttling | Count attempts per normalized email, known or unknown, reserving each before the password check; a success clears the count. After the limit, refuse that email (429) for the lockout, even with the right password. In memory | Anyone who knows an email can lock it for one lockout (15 minutes by default); accepted for a coursework deployment. Per-IP counting needs the client address through the controller and is unreliable behind a proxy. A restart clears all counts; multiple instances would each keep their own |
| Seed | Explicit opt-in, runtime passwords, missing identities/courses only, no resets | Shared destructive reseeds and committed/default passwords unsafe; #5/#44 |
| Shared foundation | Minimum auth/client/shell/course/seed/counts for C to run independently, documented interfaces for B/A reuse | Duplicate full B product implementation would create integration conflict |

## Transaction and database guarantees

Relationship/profile and room/session transition writes acquire shared advisory transaction
lock `4422026`; account lifecycle and seed acquire it exclusively. Affected user rows are
locked in ID order before parent/request rows and state/counts are rechecked. An actor
revoked while waiting fails authorization after locking. Group approval and capacity edits
take the same group lock. Polled room reads recheck active membership without row locks;
heartbeats update only an existing unexpired tab lease without locking the group. Deletion
or expiry cannot be reversed by a heartbeat: a new capacity-checked join is required.
Partial unique indexes protect unordered pending buddy pairs, active connection pairs,
pending group/applicant pairs and recipient/event identities.

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
E2 merged through [PR #52](https://github.com/aryan12singh/study-buddy-matcher/pull/52),
including dated group/buddy sessions, calendar export, agenda/dashboard events and the
subsequently approved weather enhancement. The [16 October database rollout record](DATABASE_OPERATIONS.md#e2-rollout--16-october-2026)
records its schema application; it is not evidence of a persistent hosted application.
The baseline/E2 implementation described here was checked against `9592fd2` on 18 October.
E1 AI explanations have their own contract and evidence on
[PR #61](https://github.com/aryan12singh/study-buddy-matcher/pull/61). Historical verification records
retain their original dates and scope. See the
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

## Collaborative study experience (E2 #16)

The project owner expanded PR #52 from a room foundation to the complete selected
E2 scope. Original retro components and built-in audio were already approved;
weather visuals and an optional live campus-weather provider were approved during
completion. E1 AI explanations and other discussion proposals remain separate.

| Question | Accepted choice and reason | Limits / alternative |
| --- | --- | --- |
| Room access | Active accepted members of an open group only; server rechecks on every request | Pending applicants/admins/public visitors gain no access; group membership never reveals private contact |
| Study Room entry (22 October) | User-supplied visual reference: a large personal Pomodoro with one groups sidebar, cream panels and original CSS pixel scenery; narrow screens stack the panels | Reuse existing components and group endpoints; no to-do/subject panels or third-party picture/audio integrations |
| Personal Pomodoro | Default 25-minute focus/5-minute break with whole-minute user edits (focus 1–180, break 1–60), start/pause/resume and confirmed reset; reuse timer projection and elapsed timestamps to recover after inactive tabs | Session storage holds only validated per-account timer state in the current browser tab; storage failure falls back to an in-memory timer. It does not join/control a group room or synchronize across devices |
| Shared transport | Existing authenticated HTTP with configured polling, server timestamps and local countdown rendering | No Socket.IO/new dependency; this is small-dataset synchronization, not sample-synchronized streaming |
| Presence lease | 90 seconds by default. Browsers can slow timers in long-hidden tabs to one run a minute, so a shorter lease dropped students who switched tabs | A tab that crashes without leaving holds its place for up to 90 seconds. The earlier 30-second lease expired during ordinary background-tab use and forced a rejoin |
| Host disconnect | Timer continues from durable state; co-host or leader can recover. Delegated control requires current presence; leader control remains available without a lease | Disconnection is not a timer pause or leadership transfer |
| Roles/settings | Leader alone assigns host/co-host. Present assigned hosts/co-hosts may change durations/capacity while preserving roles; durations change only from IDLE | Role reassignment by a delegated host is rejected; capacity cannot drop below present students or exceed the group |
| Revocation/readmission | `RoomLifecycle` deletes leases and revokes roles in the membership/deactivation/closure transaction | A restored membership/account does not restore an old lease or role; fresh join must satisfy capacity |
| Audio | Separate music and ambient selections/play state, with original native Web Audio synthesis; independent local volume/mute and intentional enable | No external music account/licensed asset; failure is silent and does not block the room; playback positions are local |
| Local mixer preferences | Remember only browser-local music/ambience volume and mute; validate stored types, clamp gains and use safe defaults when storage fails | No playback-enable flag or private session/account data is stored; re-entry still requires an explicit enable gesture |
| Phase cue | Announce a continuously observed automatic focus/break transition once through a polite atomic status; the changing countdown has live announcements off | Initial entry, reset, discontinuous snapshots, hidden-tab gaps and reconnect do not replay historical cues; this is not a background alarm |
| Session model | Separate `studysession` feature, with exactly one open-group or active-connection parent, DTO assembler and parent-row locking | Weekly preference schedules remain unchanged; room presence is independent of a dated booking |
| Session authority | Group leader manages group series; both participants manage their connection's buddy series; accepted group members/connected buddies can read/export | Every read rechecks current eligibility; old relationship IDs never confer renewed access |
| Local time/recurrence | Whole-minute local start/end plus IANA zone; one or up to configured 52 weekly occurrences; each occurrence preserves local wall time | Reject every missing/ambiguous DST endpoint and invalid duration; no silent timezone fallback or unlimited recurrence |
| Availability | Reuse Team A's canonical weekly intersections across current accepted members or the two buddies; return campus-local suggestions | Empty availability stays empty; no guessed attendance, reservations or enforced mutual-time booking |
| Suggested duration | Default to 60 positive whole minutes; fit the chosen duration at the beginning of each sufficiently long returned campus-local overlap | Only selecting a suggestion changes dated fields; invalid/no-fit suggestions do not block a manually entered plan or alter Team A's intersection logic |
| Session presentation | Derive current/next occurrence and Ongoing/Upcoming/Past labels from authorized occurrence timestamps, with one clock per view and focus/visibility recovery | Relative group room paths and validated HTTPS links are shortcuts only for scheduled current/future plans; joining and backend authorization remain explicit |
| JDBC civil-time mapping | Use direct local-time/local-date-time JDBC mappings for personal/group weekly slots and dated session civil fields | The E2 availability check reproduced a Singapore-JVM time shift; four narrow Team B slot-field annotations correct round trips without changing Team A maths or schema |
| Series mutation | Whole-series PUT and cancellation require observed version; notification writes share the transaction | No individual occurrence editor or invitations/RSVP workflow; cancelled series cannot be edited |
| Parent lifecycle | Ended buddy connection/closed group/deactivated account invalidates access and cancels affected scheduled series; parent deletion cascades records | Reactivation/reconnection does not resurrect schedules; surviving participants receive safe cancellation notices |
| Calendar representation | Expand the bounded series to independent UTC events with stable series UID/occurrence index and revision | No RRULE/VTIMEZONE/client-side series-edit promise; explicit dates preserve intended wall time across DST |
| Calendar privacy/import | Export title, agenda, location and validated room link; omit attendee email/contact/token payloads. Download through authenticated API | Snapshot imports can duplicate/ignore changes; shorter/cancelled series never removes offline copies automatically; no OAuth/two-way sync |
| Weather themes | Original CSS pixel scenes, selectable clear/cloudy/rain/storm/snow/fog and automatic day/night; reduced motion stops animation | Manual theme selection is decorative, never a weather observation or forecast |
| Live weather | Approved Open-Meteo current campus conditions through the backend: configured coordinates, no student data/device location/key; bounded timeout/cache with silent selectable fallback | External outage does not affect room access/timer/audio; stale/unavailable state and source attribution remain visible |

Open-Meteo is a service addition, with no new Java/npm library. The project's
non-commercial use and data attribution follow the provider's
[documentation](https://open-meteo.com/en/docs) and
[terms](https://open-meteo.com/en/terms); its weather data is attributed under
CC BY 4.0. Original room art/audio remains the team's own work. The fixed campus
weather request contains no account identity, contact, schedule, token or room data.
The test profile disables outbound weather requests. Provider/API and configuration
details belong in the README and [API contract](API_CONTRACT.md).

Implementation, local automated results, two-browser evidence, real calendar-client
imports, hosted checks, cross-team review and deployment are separate gates in
[E2_TESTING.md](E2_TESTING.md). A passing export-format test does not claim Google,
Apple or Outlook import acceptance. E2 does not fulfill E1's AI-integration work.

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
| Missing preferences | An enabled preference either student left empty is unknown: it scores `app.matching.scoring.unknown-score` (0) and the card shows "Not shared"; explicit filters exclude unconfirmed preferences | A fixed 0.5 was not neutral on a ratio scale: it let empty profiles reach Good and let "no timetable" tie with real overlap. Zero plus an honest label means missing data never outranks real compatibility; dropping unknown criteria and averaging the rest was rejected because a one-criterion profile would score 100% |
| Course in focus | Explicit search course, otherwise caller's saved target. Candidate target gives 1, enrolment-only gives `takes-course-score` (0.5), no alignment 0; with no focus, shared courses divided by the smaller course list | Saved target cannot silently replace the user's current course selection. Enrolment and buddy target remain separate concepts. The brief matches on shared courses, so with no focus the overlap of course lists ranks candidates instead of a flat 0.5; #1/#2/#7 |
| Goals | A searched goal is added to the caller's saved goals; score is shared goals / the smaller goal set (overlap coefficient). Candidate must also satisfy a selected goal filter | The filter already guarantees the searched goal, so the other saved goals are what separate candidates. Dividing by the smaller set makes saved-goal scores the same from either side and does not penalise a student open to several goals; Jaccard (shared / union) would. Students listing every goal score well against most others: accepted, see the report; #1/#2 |
| Group preference | Categorical UI maps to inclusive numeric intervals (2-2, 3-maximum, 2-maximum); scorer/filter use overlap | Brief repeats the label "preferred study mode" for group size, while Appendix A gives intervals; this reconciles both examples; #1/#2/#7 |
| Availability | Merge per-day duplicate/overlapping/adjacent slots, then intersect unions. Count only shared blocks of at least `min-session-minutes` (60), divided by the smaller of the two timetables. Same-day half-open intervals | Pairwise summation alone double-counts duplicate legacy inputs. The smaller timetable makes the score the same from either side and does not penalise listing lots of free time; a 15-minute overlap cannot hold a session; #1/#8 |
| Weekly timezone | Recurring schedules mean Asia/Singapore campus-local day/time; no conversion by browser timezone or DST | UTC is used for event timestamps, not weekly preference slots; #1/#8 |
| Disabled criteria | Global enable flags remove scoring and priority influence. Every strategy stores all five weights, disabled weights 0 and enabled sum 1; at least one enabled | Weight 0 alone cannot disable a lexicographic priority; #1/#4 |
| Strategy priority | Balanced uses total; Availability-first ranks by configured shared-hour bands and Course-first by bands at 1 and `takes-course-score`, then total; an unknown priority criterion is always in the lowest band; disabling that priority falls back to total. Defaults give the priority criterion the largest weight | Appendix A asks these strategies to prioritise their criterion before others. Sorting by its exact value let 15 extra minutes outrank everything, and a zero default weight let a card show 1.00 Strong below 0.65 Good. Bands keep the priority while the total stays meaningful. Course bands are derived from the scoring rule rather than configured separately, so changing `takes-course-score` cannot drop "takes it" into "neither" |
| Score symmetry | Availability and saved goals score the same from either side (see their rows). Course scoring and a searched goal follow the searcher's current course or goal, so they can differ by direction | A match is between two people, so shared measures should agree. Course and searched goal express what this searcher is looking for now, which is one-sided by nature |
| Stable ordering | Strategy order, then more shared hours, then case-insensitive name, then ID. Alternative sorts start with shared hours or caller-year equality, then reuse that chain | Database/insertion order and quality grouping are not deterministic ranking rules; #2/#3 |
| Search entry | Require a current course or goal. Additional filters combine with AND; personal filters/sorts remain usable when the corresponding scoring criterion is disabled and never mutate admin config | Returning all profiles for an unqualified search contradicts the current issue requirement; #2/#3 |
| Candidate eligibility | Exclude self, inactive/deleted accounts and active connections; retain pending candidates with incoming/outgoing state | Existing buddies belong on Connections; a pending request is still visible but cannot be resent; #2/#10 |
| Results bound | Default 20, maximum 50, configured in YAML. Count eligible filtered results before limiting, sort before truncation | Full pagination is unnecessary for the bounded demonstration dataset; #2 |
| Quality and threshold | Quality cut-offs remain YAML defaults; admin minimum total score and student minimum quality both apply | A user filter can narrow, never override a global threshold; #1/#2/#4 |
| Explainability | Immutable `MatchScore` holds bounded total, criterion breakdown, unique shared hours, unknown criteria and quality. `MatchResultAssembler` adds deterministic text from `MatchExplainer`, enabled metadata, relevant course and public profile data. At `9592fd2`, the card shows scores/bars/shared time; it does not render that text | Percent compatibility is not a success probability; wording does not determine ranking. E1's provider path and evidence are recorded separately on PR #61; #1/#2/#15 |
| Default/override precedence | External startup defaults apply until saved overrides exist. Explicit restore clears overrides and reuses current external defaults; overrides persist across restarts | Silent reset on each startup loses admin settings; #4 |
| Configuration version | Persisted monotonically increasing revision; expected revision required for save/restore, stale writes conflict. Search reads one repeatable snapshot and returns its version | Timestamp-only or memory-only versions cannot protect concurrent writes or future explanation caches across restart; #1/#2/#4/#15 |
| Home integration | Small matching preview reads the same search/configuration response; uses saved target or goal only to form a valid current request, with a clear first-use link when neither exists | Separate dashboard heuristics can disagree with Find buddies; changes to Team B Home are limited integration work; #2/#3/#9 |

## AI match explanation decisions (E1)

Choices for [#15](https://github.com/aryan12singh/study-buddy-matcher/issues/15), recorded
16 October and corrected after the 18 October evaluation/lifecycle audit. Team A agreed the
`MatchService.evaluate` addition and `MatchCard` button. The current
[API contract](API_CONTRACT.md#match-explanation-e1) and [test guide](E1_TESTING.md) supersede
the historical planning documents.

| Question | Implemented choice and reason | Alternative / affected work |
| --- | --- | --- |
| When is AI called? | Only after **Explain this match**, a current evaluation check, a cache miss and an available per-process quota slot | Automatic explanations multiply requests and delay search; #3 |
| Which score is explained? | The server recomputes matching and compares the card's fingerprint over exact scoring inputs/output and settings. Stale cards return 409 and refresh before another request | Matching filters alone cannot reproduce an earlier evaluation; no trusted client scores or search-session table; #1/#2 |
| What is cached? | Anonymous wording keyed by rounded evidence, prompt v5 and model. This semantic key is distinct from exact card identity; equivalent wording may be shared after freshness verification | Per-student cache keys add unnecessary personal data and deletion work; #14/#4 |
| What happens during a card refresh? | Completed/in-flight answers belong to candidate and evaluation ID; changed IDs remount the panel and abort/ignore old responses. Relationship-only changes preserve matching text | Request-send refreshes retain mounted cards, so filter/unmount handling alone is insufficient |
| Which facts reach Gemini? | Explicit scoring-course versus shared-course provenance and effective goal intent; `StudyGoalIntent` is shared with the scorer. All fields are allowlisted anonymous evidence | A search goal need not be a saved profile goal; a shared course may not be the scored target |
| What happens on failure? | Off/quota/provider/validation/cache-read failures use standard text. Cache-write failure retains validated text. Core auth/matching errors propagate | Optional caching must not make an available explanation fail; no broad exception suppression |
| How is output checked? | Fresh and cached answers pass current length and bounded evidence-reference checks; UI cautions that text may be inaccurate | This is not a complete factual validator or a guarantee against hallucination |
| How is cost bounded? | Configured daily quota is local to one process, resets on restart; after a provider 429 no calls are made for `rate-limit-cooldown` (60s). Concurrent misses may still make separate provider calls | No durable multi-instance quota or single-flight guarantee; no new infrastructure for this scope |
| Is the quota per student? | Yes, within a shared cap. Each student may trigger `daily-call-limit-per-student` (5) provider calls a day, inside the process-wide `daily-call-limit` (18). Cache hits are free. Both counts are in memory and reset at Singapore midnight | A shared allowance alone let one student clicking many uncached cards use it up for everyone; a per-student counter alone could exceed the free tier |
| How is evidence worded for the model? | `ExplanationVocabulary` sends plain words ("exam preparation", "meeting mode") and each criterion's weight; the admin strategy name is not sent. The validator rejects leftover code names (`ONE_TO_ONE`, `STRONG`) | Raw enum names were copied into student-facing text; without weights the model could lead with a criterion that barely counts |
| Why are there two hashes? | The card `evaluationId` proves the clicked card is still current; the evidence hash lets identical anonymous facts reuse wording without storing who the students are | One hash cannot do both: exact identity would stop reuse, rounded evidence cannot detect a changed card |
| What do status codes reveal? | Evaluation runs before the ID comparison, so an unknown or ineligible candidate gets 404 while an eligible one with a wrong ID gets 409. Low risk: any student can already see eligible candidates in search | Reordering is impossible: the current ID only exists after evaluation |
| Is `evaluationId` sensitive? | It is an unsalted SHA-256 of scoring inputs (including IDs and availability). Only the digest reaches the browser and the input space is large, so it is treated as low risk | An HMAC with a server secret would remove even guessing attacks; not needed at this scope |

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
