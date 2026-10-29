# Design decisions

The team's choices where the IS442 brief is silent or open to interpretation, with the reason for
each and the alternative it replaced. Where a row records an interpretation rather than a rule from
the brief, it says "Team decision". API details are in [API_CONTRACT.md](API_CONTRACT.md) and
configuration keys in the README.

## Contents

- [Accounts, relationships and groups (Team C)](#accounts-relationships-and-groups-team-c)
- [Transaction and database guarantees](#transaction-and-database-guarantees)
- [Platform and own profile (Team B)](#platform-and-own-profile-team-b)
- [Interface behaviour](#interface-behaviour)
- [Matching decisions (Team A)](#matching-decisions-team-a)
- [AI match explanation decisions (E1)](#ai-match-explanation-decisions-e1)
- [Collaborative study experience (E2)](#collaborative-study-experience-e2)
- [Study assistant AI (shared)](#study-assistant-ai-shared)
- [Pixel study assistant](#pixel-study-assistant)
- [Quizzes](#quizzes)
- [Study plans](#study-plans)
- [Music: Spotify and Freesound](#music-spotify-and-freesound)
- [Achievements](#achievements)
- [Baseline requirement contract](#baseline-requirement-contract)

## Accounts, relationships and groups (Team C)

The brief requires admin deletion without saying hard or soft. Team decision: separate Delete and
Deactivate actions.

| Decision | Choice and reason | Alternative rejected |
| --- | --- | --- |
| Leader model | The creating student is the leader: leader foreign key plus an accepted membership. STUDENT and ADMIN are the only roles | A subclass or GROUP_LEADER JWT role would misrepresent per-group permissions |
| Deactivate | Keep identity, profile and history; revoke access; end active buddies; cancel pending buddy requests; close led groups and reject their applicants; withdraw own pending applications; leave open groups, all atomically. Closed groups keep the leader and members | Flipping an active flag alone leaves usable relations behind |
| Reactivate | Fresh sign-in; no relation is restored; old JWT versions stay invalid | Restoring ended relations or old tokens would change what the other students consented to |
| Permanent delete | Remove the user, student and all dependent records, including led groups; preserve other groups; notify surviving participants generically and purge links to deleted resources | Delete-as-deactivate is not a distinct operation; orphaned leaders or implicit transfer are unsupported |
| Admin safeguards | Self-removal is blocked and the actor must be an active admin, so one always remains. The exclusive lifecycle guard serialises admins removing each other and rejects revoked actors | A check-then-write count races |
| Role changes | Chosen at creation and fixed on edit; no Student/Admin conversion | Conversion needs its own profile and data policy |
| Email identity | Trim and lower-case; unique database expression index; an inactive account keeps its email reserved, deletion releases it | A case-sensitive application-only check permits duplicates |
| Contact reads | Separate public and connected profile DTOs; contact visible to self or a current buddy only, never to group peers or in admin payloads | Entity serialisation or CSS hiding leaks the value |
| Admin contact writes | Required when an admin creates a student; optional replacement on edit, where omission keeps the stored number | Prefilling a private contact in an admin payload bypasses the read policy |
| Usage | Last successful sign-in, unended connections, current accepted membership of open groups (including led ones); history metrics are labelled separately | Online status and guessed scores are not usage evidence |
| Request context | PROFILE or MATCHING, with optional known course or goal, validated and kept | Inventing a search course from profile preferences |
| Group capacity | Accepted members including the leader count; pending requests do not. The group row serialises approval, edit, removal and close | Separate pre-counts allow over-capacity commits |
| Group closure | One-way; pending applicants rejected; other members notified; no new requests, approvals or edits. Also revokes room leases and cancels scheduled sessions | Leader transfer and reopening are not implemented |
| Taking back an action | The sender withdraws a pending buddy request (`CANCELLED`); the applicant withdraws a pending application (`WITHDRAWN`); a member leaves an open group, which also ends their study-room presence. Each confirms first and notifies the other party. The leader cannot leave and closes the group instead | Without these, a pending request blocked a pair indefinitely and deactivation had to record withdrawals as leader rejections |
| Optional group data | Goals, mode and weekly slots may be empty; name, course and capacity of at least 2 are required | Inventing compulsory choices would change a graded field requirement |
| Weekly times | Whole minutes, same-day start before end, Asia/Singapore. Group schedules may be empty or overlap; a student's own schedule rejects same-day overlap | Group meeting slots and personal availability are different contracts; overnight splitting is not supported |
| Event times | Java `Instant`, PostgreSQL `timestamptz`, `Z` timestamps in the API; legacy local values converted from Asia/Singapore | Relabelling a timestamp without conversion shifts recorded moments |
| Notifications | One service; safe summary and resource metadata; recipient scope; persistent read state; transaction and event-key deduplication | Email, SMS, push and scheduled reminders are not implemented |
| Database access | The backend's JDBC owner writes; browser table and sequence grants revoked; RLS enabled with no browser policies | Supabase Auth/RLS browser access is a different architecture from application JWTs |
| Cross-instance write order | A shared transaction advisory guard for relationship, profile, room and session transitions; exclusive for account lifecycle and seed; user rows locked in ID order before parent and request rows; actor version rechecked. Room reads and existing-lease renewals avoid the group lock | A process mutex fails with several instances; serialising every poll blocks independent work |
| Data freshness | Refetch on mutations, focus and visibility plus a bounded poll; a background refresh keeps the last response until replaced and clears it if the refresh fails; sensitive view state is purged on disconnect, logout and 401; forms keep their input | A displayed number cannot be erased from someone else's memory |
| Sign-in throttling | Attempts counted per normalised email, known or unknown, reserved before the password check; success clears the count. Past the limit, that email is refused (429) for the lockout, even with the right password. Kept in memory | Anyone who knows an email can lock it for one lockout (15 minutes by default), accepted for coursework. Per-IP counting is unreliable behind a proxy. A restart clears counts; several instances would each keep their own |
| Seed | Explicit opt-in, passwords supplied at runtime, adds missing identities and courses only, never resets | Destructive reseeds and committed default passwords are unsafe |

## Transaction and database guarantees

Relationship, profile, room and session transition writes take shared advisory transaction lock
`4422026`; account lifecycle and seed take it exclusively. Affected user rows are locked in ID order
before parent and request rows, and state and counts are rechecked. An actor revoked while waiting
fails authorisation after locking. Group approval and capacity edits take the same group lock.
Polled room reads recheck active membership without row locks; heartbeats update only an existing,
unexpired tab lease without locking the group. A heartbeat cannot reverse deletion or expiry: a new
capacity-checked join is required. Partial unique indexes protect unordered pending buddy pairs,
active connection pairs, pending group/applicant pairs and recipient/event identities.

Application exceptions map to safe RFC 9457 problems; SQL details stay internal. Notifications
commit in the same transaction as the transition that caused them. HTTP tests inject a notification
failure and verify rollback, and run simultaneous decisions against PostgreSQL. The advisory
protocol covers this backend's writes; privileged SQL run outside it can still break cross-row
capacity rules, so operators must not bypass it.

## Platform and own profile (Team B)

| Decision | Choice and reason | Alternative rejected |
| --- | --- | --- |
| Identity and profile model | `Student` composes `User` through a shared `@MapsId` identity; administrators have no student profile | User inheritance would mix account and security concerns with student-only preferences |
| Authentication | Spring Security, JJWT and BCrypt; the server checks role, active account and token version on every request | Browser route guards alone cannot enforce authorisation |
| Own profile boundary | `MyProfileAssembler` returns self-only profile and contact fields; other students' profiles go through the privacy assembler | Reusing the own-profile DTO for discovery would expose contact and email |
| Saved buddy target | Independent of course enrolments; Home uses it for the matching preview. Find buddies still needs a current course or goal | Treating it as the finder's default would contradict the screen |
| Preference representation | Categorical mode and group-size controls; group size stored as the inclusive min/max interval; optional goal set | A second competing representation would complicate matching |
| Whole-week replacement | Per-student profile and availability writes are serialised before the collection is read, deleted and reinserted; the response is the complete replacement | Two individually valid concurrent weeks could otherwise combine into an invalid one |
| Owner-editor recovery | After a successful save followed by a failed refresh, the other unsaved profile or week draft stays mounted with retry and discard. Logout, identity change and lost authorisation clear it | Relaxing the privacy purge globally would retain other students' private responses |
| Time drafts | The shared editor reports incomplete or invalid text and dirty state to profile and group forms; save is blocked until valid, and exit protection includes that text | Keeping an old valid value while showing a new invalid one silently saves the wrong input |
| Overlap policy | Personal availability rejects overlapping same-day blocks; groups permit overlap, and the editor explains which policy applies | A generic overlap warning would imply a group API rejection |
| Setup and schema | Development packaging skips tests; the test harness provisions and migrates PostgreSQL. Ordered SQL migrations are required on runtime databases | Hibernate `update` cannot replace the migration, adoption and RLS work |
| Windows and fonts | Decorative title bars and conventional routes; shared retro components; Tahoma/Verdana/system body fonts; original CSS pixel art; narrow layouts wrap | A movable window manager or pixel font adds no object-oriented value |

The whole-system boundary and platform use cases are in [diagrams/PLATFORM.md](diagrams/PLATFORM.md).

## Interface behaviour

| Choice | Behaviour |
| --- | --- |
| One data router | `routes.tsx` holds the guarded route paths; `main.tsx` creates the browser router once, which enables the router's navigation blocker without custom history handling |
| Unsaved drafts | Group and account edits and buddy/application dialogs prompt on dirty in-app exits, Cancel/X/Escape and reload. Leaving during a pending write does not cancel it; a late completion cannot redirect or call back into an abandoned dialog |
| Destination feedback | Create/update/delete messages show at the destination, read once from history state, dismissible; Back and reload do not replay them; no account, contact, password or draft values appear in them |
| URL views | Groups use `view`, `courseId`, `studyGoal`, `studyMode`; notifications use `view`. Tabs push history; filter edits replace it. Invalid values fall back safely; unrelated query keys are kept |
| Weekly editing | Duplicate creates an independent block; copy adds it to other selected days without duplicating an identical block. The preview sorts for display without reordering the input |
| Copy buttons | An explicit click copies the canonical group URL, or an already-authorised contact. A refused clipboard gives manual guidance; the contact is never duplicated into the fallback or stored |
| Honest decoration | Pixel patterns and name initials imply no identity or presence; capacity uses server counts; loading stays indeterminate. No made-up percentages or online indicators |
| Narrow layouts | Admin tables keep row/cell roles when shown as cards; labels, actions, wrapping, keyboard focus and reduced motion are retained |
| Nested dialogs | Scroll stays locked until the last modal closes, even when closed out of order; each dialog keeps its own focus and Escape handling |

## Matching decisions (Team A)

Appendix A lets each group choose its formulas and weights. These are the team's choices.

| Question | Choice and reason | Alternative rejected |
| --- | --- | --- |
| Which criteria? | Five independent `CriterionScorer` implementations (course, availability, meeting mode, goals, group size), each returning a `CriterionResult` from 0 to 1 or unknown | Monolithic conditional scoring makes strategies hard to explain |
| Missing preferences | An enabled preference either student left empty is unknown: it scores `app.matching.scoring.unknown-score` (0) and the card shows "Not shared". Explicit filters exclude unconfirmed preferences | A fixed 0.5 is not neutral on a ratio scale: it let empty profiles reach Good and "no timetable" tie with real overlap. Dropping unknown criteria and averaging the rest would let a one-criterion profile score 100% |
| Course in focus | The search course, otherwise the caller's saved target. Candidate's target matches: 1; enrolled only: `takes-course-score` (0.5); neither: 0. With no focus, shared courses divided by the smaller course list | The brief matches on shared courses, plural; with no focus, a flat 0.5 for everyone ranks nothing. Enrolment and buddy target stay separate concepts |
| Goals | A searched goal is added to the caller's saved goals; score is shared goals divided by the smaller goal set (overlap coefficient). The candidate must also satisfy a selected goal filter | The filter already guarantees the searched goal, so the other saved goals separate candidates. Jaccard (shared / union) would penalise a student open to several goals |
| Group size | Categorical UI maps to inclusive intervals (2-2, 3-maximum, 2-maximum); scorer and filter use interval overlap | The brief labels group size as "preferred study mode" while Appendix A gives intervals; this reconciles both |
| Availability | Merge each day's duplicate, overlapping and adjacent slots, then intersect. Count only shared blocks of at least `min-session-minutes` (60), divided by the smaller timetable. Same-day half-open intervals | Pairwise summation double-counts duplicates; a 15-minute overlap cannot hold a session; dividing by the searcher's timetable punished students who list more free time |
| Weekly timezone | Recurring schedules are Asia/Singapore campus-local day and time; no browser-timezone or DST conversion | UTC is for event timestamps, not weekly preference slots |
| Disabled criteria | Global enable flags remove a criterion from scoring and priority. Every strategy stores all five weights; disabled weights are 0 and enabled weights sum to 1; at least one is enabled | Weight 0 alone cannot disable a priority ordering |
| Strategy priority | Balanced ranks by total. Availability-first ranks by shared-hour bands (`priority-tiers.availability-hours`, 3 h and 1 h), Course-first by bands at 1 and `takes-course-score`, then by total within a band. Unknown priority data is always in the lowest band; disabling the priority criterion falls back to total. Defaults give the priority criterion the largest weight | Sorting by the exact value let 15 extra minutes outrank everything, and a zero default weight let "1.00 Strong" rank below "0.65 Good". Weight presets alone would not prioritise the criterion as Appendix A asks. Course bands are derived from the scoring rule, so changing `takes-course-score` cannot misplace students |
| Band values | One hour is the shortest useful session; three hours is more than one session a week. Appendix A's 2-hour overlap falls in the middle band | Within the top band a lower total can appear above a higher one from a lower band; that is the strategy's purpose, and the results header explains the grouping |
| Score symmetry | Availability and saved goals score the same from either side. Course and a searched goal follow the searcher's current intent, so they can differ by direction | A match is mutual, so shared measures agree; what a searcher is looking for now is one-sided by nature |
| Openness | Dividing by the smaller set means a student listing every goal or a very open timetable scores well against most others | Accepted: openness is not penalised, matching how "either" is treated for mode. Jaccard would penalise it instead |
| Stable ordering | Sort option, then strategy, then more shared hours, then case-insensitive name, then ID. Each `MatchSort` value supplies its own comparator | A switch on the sort type; alphabetical ties are not something a student cares about |
| Search entry | A current course or goal is required. Filters combine with AND; personal filters and sorts work even when the matching scoring criterion is disabled, and never change admin settings | Returning every profile for an unqualified search |
| Candidate eligibility | Exclude self, inactive or deleted accounts and active connections; keep pending candidates with their incoming/outgoing state | Existing buddies belong on Connections; a pending request is visible but cannot be resent |
| Results bound | Default 20, maximum 50, from YAML. Count filtered results before limiting; sort before truncating | Full pagination is unnecessary for the bounded dataset |
| Quality and threshold | Quality cut-offs are YAML defaults; the admin minimum total and the student's minimum quality both apply | A user filter can narrow but never override a global threshold |
| Explainability | Immutable `MatchScore` holds the bounded total, breakdown, shared hours, unknown criteria and quality. `MatchExplainer` (`TemplateMatchExplainer`) writes deterministic text from the score alone; `MatchResultAssembler` adds it with public profile data. The card shows scores, bars and shared time, and offers the E1 explanation on request | Scoring and wording change for different reasons. Because the explainer sees only the score, never a `Student`, it cannot leak a private field |
| Query count | `StudentRepository.findWithMatchingDetailsByIdIn` loads candidates with an `@EntityGraph`; `AvailabilitySlotRepository.findByStudentIdIn` loads slots for eligible students only. Statement count stays constant as the pool grows (`MatchingQueryCountTest`) | A global `default_batch_fetch_size` still grows with the pool, loads inactive students' slots and changes how every entity loads |
| Defaults and overrides | YAML defaults apply until an admin saves overrides; overrides persist across restarts; Restore clears them and reuses current YAML defaults | Resetting on every startup loses admin settings. Saved overrides keep their own weights until restored |
| Configuration version | A persisted, monotonically increasing revision; save and restore require the expected revision and stale writes conflict. Search reads one repeatable snapshot and returns its version | Timestamp or memory-only versions cannot protect concurrent writes across restarts |
| Home preview | Reads the same search and configuration response, using the saved target or goal only to form a valid request, with a first-use link when neither exists | Separate dashboard heuristics could disagree with Find buddies |

Not implemented, by choice: related or equivalent courses; a separate matching profile apart from
the `Student` entity; admin-screen fields for unknown score, bands and session length (they are in
`application.yml`); learning style as a sixth criterion (see the
[baseline contract](#baseline-requirement-contract); adding one would be a new `CriterionScorer`,
enum value and weight row). Team decision: exact shared time blocks are visible in match results;
only the contact number is withheld until a request is accepted.

## AI match explanation decisions (E1)

| Question | Choice and reason | Alternative rejected |
| --- | --- | --- |
| When is AI called? | Only after **Explain this match**, a current-evaluation check, a cache miss and an available quota slot | Automatic explanations multiply requests and slow search |
| Which score is explained? | The server recomputes matching and compares the card's fingerprint (`MatchEvaluationFingerprint`) over the exact scoring inputs, output and settings. A stale card returns 409 and refreshes | Trusting client scores, or a search-session table |
| What is cached? | Anonymous wording keyed by rounded evidence, prompt version and model; distinct from exact card identity, so equivalent wording is shared after the freshness check | Per-student cache keys add personal data and deletion work |
| Card refreshes | Answers belong to the candidate and evaluation ID; a changed ID remounts the panel and aborts or ignores old responses. Relationship-only changes keep the text | Filter or unmount handling alone misses refreshes after a request is sent |
| Which facts reach Gemini? | Allowlisted anonymous evidence only, with scoring course distinguished from shared course and the effective goal intent (`StudyGoalIntent`, shared with the scorer) | A search goal need not be a saved goal; a shared course may not be the scored one |
| Failure | Off, quota, provider, validation and cache-read failures return the standard template text. A cache-write failure keeps the validated text. Core auth and matching errors propagate | Optional caching must not make an available explanation fail |
| Output checks | Fresh and cached answers pass length and bounded evidence-reference checks; the UI cautions that text may be inaccurate | Not a complete factual validator |
| Cost | A daily quota per process, reset at Singapore midnight: `daily-call-limit` (18) shared, `daily-call-limit-per-student` (5) each; cache hits are free. After a provider 429, no calls for `rate-limit-cooldown` (60 s). Concurrent misses may each call | A shared allowance alone lets one student use it up; a per-student counter alone could exceed the free tier |
| Evidence wording | `ExplanationVocabulary` sends plain words and each criterion's weight; the strategy name is not sent. The validator rejects leftover code names such as `ONE_TO_ONE` | Raw enum names were copied into student-facing text; without weights the model could lead with a criterion that barely counts |
| Two hashes | The `evaluationId` proves the clicked card is current; the evidence hash lets identical anonymous facts reuse wording | One hash cannot do both |
| Status codes | Evaluation runs before the ID comparison, so an ineligible candidate gets 404 and an eligible one with a wrong ID gets 409. Low risk: eligible candidates are already visible in search | The current ID only exists after evaluation |
| `evaluationId` sensitivity | An unsalted SHA-256 of scoring inputs; only the digest reaches the browser and the input space is large | An HMAC with a server secret, not needed at this scope |

## Collaborative study experience (E2)

### What E2 delivers

| Component | Behaviour |
| --- | --- |
| Private group room | Accepted active members only; join/leave, participant list, focus/break state, distinct-student capacity and expiring tab leases |
| Shared Pomodoro | Configurable durations; server-time start, pause, resume and reset; observed-version conflict checks; survives refresh and reconnect |
| Leader and delegated control | The leader assigns host and co-host; present hosts control the timer and configure durations and capacity; the leader keeps recovery control |
| Personal Pomodoro | A personal Study Room with its own timer, independent of any group room |
| Music | Each student's own Spotify and Freesound player in the room's Music tab (see [Music](#music-spotify-and-freesound)); nothing reaches the room or other members |
| Dated sessions | Group leaders and both connected buddies manage dated series with timezone, title, optional agenda, location and room link, and whole-series update or cancel |
| Mutual availability | Team A's interval intersections across current participants; suggestions never confirm attendance |
| Calendar export | Single or bounded weekly series expanded to stable individual UTC events; authenticated download |
| Product integration | Sessions on Home and group/buddy agendas; recipient-scoped change notifications; closure and lifecycle revoke access |
| Weather | Original pixel themes, day/night, and optional backend-cached Open-Meteo campus conditions with a selectable fallback |

Not part of E2: public rooms, chat, task boards, shared notes, global online status, group-score
recommendations, calendar OAuth or two-way sync, and scheduled reminders.

### Decisions

| Question | Choice and reason | Limits / alternative |
| --- | --- | --- |
| Room access | Active accepted members of an open group only; rechecked on every request | Pending applicants, admins and visitors have no access; membership never reveals contacts |
| Personal Pomodoro | Default 25-minute focus and 5-minute break, whole-minute edits (focus 1-180, break 1-60), start/pause/resume and confirmed reset; recovers after inactive tabs from elapsed timestamps | Session storage holds only validated per-account timer state in the current tab, falling back to memory. It does not control a group room or synchronise across devices |
| Shared transport | Authenticated HTTP with configured polling, server timestamps and local countdown rendering | No new socket dependency; this is small-dataset synchronisation |
| Timer model | An immutable server-time anchor plus remaining duration, not ticks | Ticks drift and cannot survive reconnect |
| Presence lease | 90 seconds by default, longer than the one-minute timer floor browsers apply to hidden tabs. Several tabs count once; leaving removes only that tab's lease | A crashed tab holds its place for up to 90 seconds; a shorter lease dropped students who switched tabs |
| Host disconnect | The timer continues. A departing or expired host hands over to the present co-host, else the most recently active person still there; the last person leaving ends the session and resets the timer. Leadership never transfers | Without a background job, an expired tab counts as leaving only when someone next joins |
| Roles and settings | The leader alone assigns host and co-host. Present hosts may change durations (only from IDLE) and capacity, without changing roles | Capacity cannot drop below the students present or exceed the group |
| Revocation | `RoomLifecycle` deletes leases and revokes roles in the membership, deactivation and closure transaction | A restored membership never restores an old lease or role |
| Phase cue | A continuously observed automatic focus/break change is announced once through a polite status; the countdown itself is not announced | Entry, reset, hidden-tab gaps and reconnect do not replay cues; not a background alarm |
| Room audio | Rooms hold no audio state | The unused `study_rooms` audio columns remain until a later migration |
| Session model | A separate `studysession` feature with exactly one parent (open group or active connection), its own assembler and parent-row locking | Weekly preference schedules unchanged; room presence is independent of a booking |
| Session authority | The group leader manages group series; both buddies manage their connection's series; members and buddies can read and export | Every read rechecks current eligibility; old relationship IDs confer nothing |
| Local time and recurrence | Whole-minute local start and end plus an IANA zone; one occurrence or up to `maximum-occurrences` (52) weekly ones, each keeping its local wall time | Missing or ambiguous DST endpoints are rejected; no silent timezone fallback |
| Suggested times | Reuse campus-local weekly intersections; default 60-minute duration placed at the start of each long-enough overlap | Selecting a suggestion only fills the form; manual times still work |
| Session labels | Current/next occurrence and Ongoing/Upcoming/Past from authorised timestamps, one clock per view | Room links are shortcuts only for current or future plans |
| JDBC civil time | Local-time JDBC mappings for weekly slots and dated session civil fields | A Singapore-timezone JVM otherwise shifted times on round trip |
| Series changes | Whole-series PUT and cancel require the observed version; notifications share the transaction | No per-occurrence editing, invitations or RSVP; cancelled series cannot be edited |
| Parent lifecycle | An ended connection, closed group or deactivated account cancels affected scheduled series; deletion cascades | Reconnection does not resurrect schedules |
| Calendar export | Independent UTC events with a stable series UID, occurrence index and revision; title, agenda, location and validated room link only, no emails, contacts or tokens | Snapshot imports can duplicate or miss edits; shortening or cancelling a series never removes an already imported copy |
| Weather themes | Original CSS pixel scenes (clear, cloudy, rain, storm, snow, fog) and automatic day/night; reduced motion stops animation | A manually chosen theme is decoration, never a forecast |
| Live weather | Open-Meteo current conditions for configured campus coordinates through the backend, with timeout, cache, stale display and silent fallback. No student data, device location or key is sent | An outage never affects room access or the timer. Weather data is attributed under CC BY 4.0 per the provider's [terms](https://open-meteo.com/en/terms); the test profile disables outbound requests |

## Study assistant AI (shared)

Pixel, quizzes and study plans share one Gemini integration in `com.studybuddy.ai`, separate from
E1's `matchexplanation` package.

| Decision | Choice and reason |
| --- | --- |
| Separate project and quota | `app.ai.assistant.*` has its own API key (from a different Google project than E1) and its own `AiQuota`, so assistant use never spends E1's allowance. Off unless `application-local.yml` sets `enabled: true` and `api-key` |
| Provider behind an interface | Features depend on `AiClient`; `GeminiClient` is the implementation, so tests supply a fake |
| Quota | Per process, in memory, reset at Singapore midnight: `daily-call-limit` (18) shared and `daily-call-limit-per-student` (5). After a provider 429, calls pause for `rate-limit-cooldown`. A disabled assistant is checked before the quota, so it never spends allowance |
| JSON replies | Requests ask for `application/json`; a reply that did not finish normally (token limit, safety block) is rejected rather than parsed |
| Long requests | `timeout` 90 s and `max-output-tokens` 4000, because a quiz or roadmap from a lecture PDF can take Gemini well over 30 seconds |
| No transaction during the call | Creation runs as a short checking read, the AI call, then a short write, so no connection or account lock is held while Gemini answers |
| Validate before spending | Every check that needs no AI runs first, so a bad request never uses the student's allowance |
| Versioned prompts | Templates live in `backend/src/main/resources/prompts/` (`assistant-v2.txt`, `quiz-v1.txt`, `study-plan-v2.txt`); a wording change is a new file and version |
| Prompt injection | Student text and material are marked as data, never instructions; slots are filled in a single pass so student text is never searched for slots |
| Failure | Any failure is `AiUnavailableException` (503 `AI_UNAVAILABLE`); logs never contain the prompt, file, reply or key |

## Pixel study assistant

**What it does.** A floating chat window for students (`AssistantLauncher`, in `AppShell`) that
answers from the app's own data. `AssistantService` works out what a message asks for, then the
matching `AssistantIntent` answers it:

| Intent | Answer |
| --- | --- |
| `MY_TO_DO` | Buddy requests to answer, applications to groups the student leads, open group quizzes not yet taken, sessions in the next seven days and unread notifications, each linked |
| `NAVIGATE` | A link to one page from the fixed `AppPage` list |
| `FIND_BUDDIES` | The real match search with the stated filters, showing `matches-shown` (3) results; asks for a course when none is given |
| `WHEN_TO_MEET` | Weekly times every member of the named open group is free, filtered by meeting length, with a link to schedule a session |
| `QUIZ_ME` | Opens Quiz Buddy with the course and topic filled in |
| `PLAN_STUDY` | A link to study plans carrying course, exam date and weekly hours |
| `PLAN_SESSION` | The student's soonest upcoming session (narrowed by group, course or meeting day), linking to its page to plan its roadmap; someone who may not manage it is told the group leader can |
| `SESSION_TOPICS` | That meeting's roadmap entries with their planned times, or why there are none (no session that day, no roadmap yet, or the roadmap no longer covers it) |
| `UNKNOWN` | Greetings and anything else |

| Decision | Choice and reason |
| --- | --- |
| AI reads, the app answers | The only AI call classifies the message and extracts details (`IntentParser`); every fact in the reply comes from existing services, so the model cannot invent a buddy, time or task |
| Deterministic fast path | `QuickIntentReader` answers the suggested questions, greetings, "what do I need to do?", navigation questions and simple buddy, meeting and quiz requests with rules and no AI call. It never guesses: a message with a time of day, date, goal or other detail only the AI can read goes to the AI |
| Lighter model, own allowance | `app.ai.assistant.chat.*` uses `gemini-3.5-flash-lite` with its own limits (200 shared, 30 per student, 20 s timeout), so chatting never spends the allowance needed for quizzes and plans. It shares the key, switch and rate-limit pause |
| Reply validation | `AssistantOutputValidator` requires a JSON object with a known intent; each detail is kept only if valid (known enum, whole number in range, readable date), otherwise dropped rather than guessed |
| Intents as classes | Intents are looked up by type, not chosen in a `switch`; the service refuses to start if any type lacks a handler |
| Page allowlist | Navigation targets come from the `AppPage` enum, so the model cannot produce an arbitrary link |
| Stateless | Each message is handled on its own, with no stored history |

**Privacy.** Only the message text and today's date reach Gemini, never a name, email or other
student's data. `<message>` markers are stripped from the text. Buddy results use the same
privacy-safe match DTOs as Find buddies.

**Limits.** Students only; messages up to `max-message-characters` (500). Time of day is not a
search filter, so a request mentioning free time sorts by most shared hours instead. When the AI is
unavailable, messages that need it return 503 and the fast path still works. The study plan link
targets `/study-plan` with the stated course, exam date and hours as query parameters, which the
Study plans screen uses to prefill its create form.

## Quizzes

**What it does.** Quiz Buddy (`QuizBotLauncher`, `QuizBotWindow`) writes a ten-question
multiple-choice quiz from uploaded or pasted notes. A personal quiz is the student's own practice,
from the personal Study Room. A group quiz is started in a group's study room and may be for one of the group's upcoming
sessions; members take it, see a leaderboard and review answers after it closes.

| Decision | Choice and reason |
| --- | --- |
| Who may start | Personal: the student. Group: the leader and the room's host and co-host (`QuizAccess`), reusing the room rules |
| Who may see | Personal: the owner alone. Group: accepted members of an open group. An invisible quiz is 404 so its ID reveals nothing; a group's quiz list is 403, like its room and sessions |
| Output checks | `QuizOutputValidator` accepts exactly `question-count` (10) questions, each with a prompt, four distinct options, one correct option from 0 to 3 and an explanation, all fitting their columns. These are shape checks; they cannot prove a question is right |
| Answers stay on the server | `QuizQuestionsDto` carries no correct option or explanation; review is available only once nobody can submit |
| Timing | A group quiz is open for `open-duration` (15 min) and reads as closed afterwards, so no scheduler is needed; hosts can close it early. A personal quiz has no time limit and closes when submitted, so the owner sees answers straight away |
| Attempts | Starting records the time; asking again returns the same attempt, so a reload does not reset the clock. The member's row is locked so a double click cannot start two |
| Marking | `QuizMarker` requires every question answered exactly once with a valid option; anything else is invalid input, so skipping cannot save time |
| Leaderboard | `QuizLeaderboard` ranks by score, then less time, then earlier submission, then student ID, so ranks are distinct and stable between polls. A member who leaves, is removed or is deactivated keeps their attempt but drops off |
| Quiz for a session | When the group has upcoming scheduled sessions, Quiz Buddy asks which one the quiz is for, with one button per session and "Not for a session"; the quiz list then names it. The question is optional and is skipped when the group has none or the list fails to load, so it never blocks a quiz. The server accepts only a scheduled session of the same group |
| Session cancelled during the AI call | The quiz is saved without a session rather than failing |

**Privacy.** Material is sent to Gemini for that one request and never stored; the quiz keeps only
its questions and a title (the focus, or the file names). The prompt forbids questions about the
students or the group.

**Limits.** Same upload rules as study plans. Group quizzes are not announced by notification;
members see open quizzes in the room and in Pixel's to-do list.

## Study plans

**What it does.** A roadmap written from the student's own material, in two kinds:

- **Personal** (`StudyPlanScope.PERSONAL`): week by week for a course and study goal. Without an
  exam date it covers this week; with one it covers every week from today up to the exam (the last
  possibly a part week), each holding the student's weekly hours. A student may keep several.
- **Session** (`StudyPlanScope.SESSION`): one plan per study session, covering its meetings that
  have not ended yet, up to `max-session-meetings` (12), each block one meeting's length. It is
  planned from the session page (`SessionRoadmapPanel`).

Entries can be ticked off and the plan deleted.

| Decision | Choice and reason |
| --- | --- |
| Material, not model knowledge | Gemini first lists the topics the material covers, in order, at most `max-source-topics` (40), then plans only those, revising earlier topics in spare weeks |
| Frame from the server | `StudyPlanCalendar` decides the weeks in Asia/Singapore, within `max-weeks` (16) and `max-hours-per-week` (40); `RoadmapFrame` gives Gemini each block and its minute limit |
| Output checks | `StudyPlanOutputValidator` accepts a reply only if every block of the frame is covered, no entry names a block outside it, each block has 1 to `max-entries-per-block` (5) entries with at most `max-tasks-per-entry` (5) tasks, minutes fit the block and every text fits its column. It cannot prove the plan is good advice |
| Upload type checks | `StudyMaterialReader` accepts files or pasted topics, not both: up to `max-files` (6), `max-upload-size` (10 MB) in total. Type is decided by extension and content together: a `.pdf` must start with the PDF signature, a `.txt` or `.md` file must be strict UTF-8 without NUL characters. Anything else is 415; too large is 413 |
| Size limits | Spring's multipart limit reads the same `max-upload-size`, so they cannot drift apart. Text files together hold at most `max-text-file-characters` (100,000); pasted topics at most `max-topics-characters` (4000) |
| Immutable plan | A plan is built in one go from a validated draft; afterwards it can only be ticked off or deleted |
| Session plans follow session access | Every current participant reads it (accepted members of an open group, or both buddies); whoever manages the session (the group leader, or either buddy) plans, ticks and deletes it. Reusing the session's rules means no second permission model |
| One plan per session | A new session plan replaces the old one, and its progress goes with it; if Gemini fails the old plan is kept. Editing the session does not regenerate it, so the screen compares the session's `updatedAt` with the plan's `createdAt` to flag a possibly outdated plan. A session cancelled or rescheduled while Gemini answers saves nothing (409) |
| Course for a session | A group session studies its group's course; a buddy connection has none, so the buddy planning it chooses one |

**Privacy.** A personal plan belongs to its student alone and a session plan to the session's
current participants; anyone else gets 404, and session plans are never listed with personal ones. The material is sent to
Gemini for one request and never stored: the plan keeps the extracted topic titles and file names
only.

**Limits.** A session plan is not regenerated when the session changes; it is flagged as possibly
out of date instead. Plans bound the shape and size of Gemini's advice, not its quality.

## Music: Spotify and Freesound

**What it does.** Both study rooms have a personal Music tab (`MusicPlayer` in the room settings
menu) and a corner mini player (`NowPlayingMini`: track, next, music and ambient volume).

- **Spotify.** Without an account link, a team-chosen playlist plays through Spotify's free embed.
  A student can connect their Spotify account and search playlists; with Premium they play in the
  browser through the Web Playback SDK.
- **Ambient sounds.** Six Freesound categories (rain, cafe, forest, ocean, fireplace, wind), each
  playing a random suitable sound, with "try another sound" and a one-time remix that layers two
  or three categories at random volumes (`remixPlan.ts`).

| Decision | Choice and reason |
| --- | --- |
| Personal, not shared | Music plays only in the student's own browser and never enters the room API |
| Tokens in memory only | `SpotifyTokenStore` keeps tokens in server memory, never in the database; a restart asks students to connect again. Only a short-lived access token reaches the browser; the refresh token and client secret never leave the server |
| Sign-in callback | Spotify's redirect carries no app token, so a random, single-use, expiring `state` (`SpotifyAuthorizationStore`) ties the callback to the student and stops another site completing a sign-in |
| CC0 licence | Only Creative Commons 0 sounds are requested, so no per-author credit is needed on screen |
| Sound length | `min-duration-seconds` (30) to `max-duration-seconds` (600): shorter clips loop audibly, longer ones start slowly on campus wifi |
| Freesound key | Sent in the `Authorization` header, never the URL, so it stays out of request logs |
| Provider behind an interface | `AmbientSoundService` depends on `SoundSearchClient`; `FreesoundClient` is the implementation |
| Browser storage | `localStorage` remembers only which Spotify player the student chose |

**Privacy.** No account data is sent to Freesound. Spotify receives only what the student's own
Spotify sign-in authorises.

**Limits.** Spotify is off until `app.spotify.client-id` and `client-secret` are set; sounds are
unavailable until `app.ambient.api-key` is set (503). In-browser playback needs Spotify Premium; the
embed works without it.

## Achievements

**What it does.** A private page of six badges with bronze, silver and gold tiers, points, a capped
level and a planned-session weekly streak shown as a growing grove (`AchievementsPage`), plus a
summary on the student's own profile.

| Badge | Counts |
| --- | --- |
| Connector | Active buddy connections (falls when disconnecting) |
| First Move | Sent match requests in any status |
| Team Player | Open groups the student belongs to without leading |
| Founder | Groups created, including closed ones |
| Planner | Planned occurrences on active buddy and group schedules from the student's current join or connection date, excluding cancelled sessions |
| Grove Keeper | Longest run of consecutive weeks with a planned session |

| Decision | Choice and reason |
| --- | --- |
| Derived counts | `AchievementService` recomputes every count from connections, requests, groups and sessions on each read, so progress always matches the records |
| Current versus history | Some badges count current standing (connections, memberships) and their counts fall; others count retained history (sent requests, created groups). Each badge states which |
| Earned tiers are kept | `EarnedBadgeRecorder` saves the highest tier each badge reaches in `earned_badges`, so losing a connection or leaving a group never takes away a medal, points or level. Students expect an earned achievement to stay earned; letting tiers fall felt like a penalty for other people's actions. A badge whose count has fallen shows what its current count alone reaches (`currentTier`) |
| When a tier is saved | When the student opens their achievements (page or own-profile summary), in the same request. Hooking every connection, group and session service instead would cross team boundaries, so a tier reached and lost between two visits is not recorded |
| Never lower | The save is one upsert whose condition keeps the higher tier, so concurrent requests cannot lower a tier. Rows cascade on permanent deletion and stay through deactivation |
| Streak rule | `WeeklyStreakCalculator` counts weeks starting Monday in `app.achievements.week-zone`; a session later this week counts now, future weeks are ignored, and a run ending last week is still current |
| Tiers and level | `BadgeEvaluator` maps counts onto configured thresholds; points come from the highest tiers and `points-per-level` sets the level. Zero or unsorted thresholds are rejected at startup |
| Batched query | `AchievementSessionRepository` loads schedule projections in one query rather than one per schedule |

**Privacy.** Students only, for themselves: no endpoint reads another student's achievements and no
achievement fields appear in profile DTOs. The profile summary is fetched only for the signed-in
owner.

**Limits.** No attendance or completed-focus tracking: a planned session counts whether or not it
was attended. Tiers are saved only on a visit, as above.

## Baseline requirement contract

### Contact-number format

Team decision: accept 8 ASCII digits starting with 6, 8 or 9 for a local Singapore number, or `+`
followed by 7-15 ASCII digits whose first digit is 1-9 for an international number. ASCII
whitespace, hyphens, parentheses and dots are removed. Local input is stored as `+65` plus the
digits; international input keeps its leading `+`. Letters, extensions, repeated or misplaced `+`,
other punctuation and out-of-range lengths are rejected. This checks representation only, not
whether the number exists.

`InputRules.contactNumber` governs registration, admin creation, own-profile replacement and
optional admin replacement; the frontend uses the same grammar. Admin omission keeps the stored
contact and never exposes it. Legacy contacts are not rewritten, but any replacement must use the
accepted format. Seed records use accepted formats.

Accepting any non-blank text would leave format validation undone; Singapore-only would exclude
international students; a phone-lookup library is unnecessary for a format check.

### Learning style

The objective names "preferred learning style", but the field list and Appendix A define no
separate taxonomy or scorer. Team decision: learning style is expressed through the defined study
preferences (meeting mode, study goals and group size, alongside course and availability). No extra
field, invented categories or sixth criterion is added. A separate field would need its categories,
API, scoring and validation agreed by Teams A and B first.
