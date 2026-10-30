# Configuration

Every tunable value lives in `backend/src/main/resources/application.yml` and is bound to a typed
configuration object, so nothing tunable is a constant in code. Set the environment variable, or
the property in the git-ignored `application-local.yml`. Secrets (database password, JWT secret,
provider keys) are set only there or in the process environment.

## Platform and security

| Key | Default | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | Loads `application-local.yml` in development |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Required unless set locally | Backend database connection |
| `DB_MAX_POOL_SIZE` / `spring.datasource.hikari.maximum-pool-size` | `5` | Most database connections one backend keeps open. Spring's default of 10 would let one developer fill most of the shared Supabase Session pooler's 15 clients |
| `JWT_SECRET` / `jwt.secret` | Required, at least 32 characters | Signs sign-in tokens |
| `JWT_EXPIRATION_MS` / `jwt.expiration-ms` | `86400000` (24 hours) | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Allowed frontend origins |
| `app.auth.secret`, `app.auth.token-lifetime`, `app.auth.allowed-origins` | Bound from the three keys above | Typed authentication settings (`AuthProperties`) |
| `LOGIN_MAX_FAILURES` / `app.auth.login-throttle.max-failures` | `5` | Wrong passwords in a row for one email before its sign-in pauses |
| `LOGIN_LOCKOUT` / `app.auth.login-throttle.lockout` | `15m` | How long sign-in for that email is refused, counted from its last unsuccessful attempt. Kept in memory, so a restart clears it |
| `PROFILE_GROUP_SIZE_MAX` / `app.profile.group-size-max` | `5` (minimum `3`) | Largest group a "small group" or "either" preference covers |
| `PROFILE_MAX_COURSES` / `app.profile.max-courses` | `8` | Most courses a student can list |
| `PROFILE_YEAR_OF_STUDY_MAX` / `app.profile.year-of-study-max` | `6` | Highest year of study a student can enter; the forms mirror this value |
| `AVAILABILITY_MAX_WEEKLY_SLOTS` / `app.availability.max-weekly-slots` | `50` | Most weekly time blocks one student or group may save; more get 400 |
| `STUDY_GROUP_MAX_CAPACITY` / `app.study-group.max-capacity` | `10` | Largest group size a leader may set; more gets 400 |
| `HTTP_MAX_JSON_BODY_SIZE` / `app.http.max-json-body-size` | `1MB` | Largest JSON request body; more gets 413 `REQUEST_TOO_LARGE`. File uploads keep their own limits |
| `spring.jackson.deserialization.accept-float-as-int` | `false` | A fraction sent for a whole-number field (an ID or a count) is rejected with 400 instead of being rounded |
| `app.achievements.week-zone` | `Asia/Singapore` | Zone whose Monday starts each planned-session week; later this week counts, future weeks wait |
| `app.achievements.points-per-level` | `3` | Positive badge points per level; only the highest tier counts (bronze 1, silver 2, gold 3); the attainable final step caps at all Gold |
| `app.achievements.connections` / `requests-sent` / `groups-joined` / `groups-led` | `1/3/5`, `1/5/10`, `1/2/3`, `1/2/3` | Bronze/silver/gold counts for the Connector, First Move, Team Player and Founder badges |
| `app.achievements.sessions-planned` / `streak-weeks` | `1/5/10`, `2/4/8` | Bronze/silver/gold counts for the Planner and Grove Keeper badges |
| `app.achievements.trees-planted` | `1/5/15` | Bronze/silver/gold tree counts for the Forester badge |

## Matching

| Key | Default | Purpose |
| --- | --- | --- |
| `app.matching.default-strategy` | `BALANCED` | Strategy until an admin chooses one (`BALANCED`, `AVAILABILITY_FIRST`, `COURSE_FIRST`) |
| `app.matching.default-weights.<strategy>.*` | Balanced: course `0.30`, availability `0.30`, study-mode `0.20`, study-goal `0.10`, group-size `0.10`. Availability first: availability `0.40`, course `0.25`, study-mode `0.15`, study-goal `0.10`, group-size `0.10`. Course first: course `0.40`, availability `0.25`, study-mode `0.15`, study-goal `0.10`, group-size `0.10` | All five weights per strategy, each 0 to 1. Enabled weights sum to 1; disabled weights are 0. A priority strategy gives its own criterion the largest share |
| `app.matching.priority-tiers.availability-hours` | `[3.0, 1.0]` | Shared-hour band boundaries for Availability first, highest first |
| `app.matching.default-enabled-criteria` | All five | Criteria enabled at startup |
| `app.matching.default-min-score` | `0` | Minimum compatibility returned |
| `app.matching.default-result-limit` / `max-result-limit` | `20` / `50` | Default and largest result count |
| `app.matching.quality.strong` / `quality.good` | `0.75` / `0.50` | Cut-offs for the Strong and Good labels; lower is Fair |
| `app.matching.scoring.unknown-score` | `0.0` | Score for a criterion either student left empty |
| `app.matching.scoring.takes-course-score` | `0.5` | Course score when the candidate takes the course but wants a buddy for another; also the lower Course-first band |
| `app.matching.scoring.min-session-minutes` | `60` | Shortest shared block that counts as time to meet |

## AI match explanations

| Key | Default | Purpose |
| --- | --- | --- |
| `app.ai.explanation.enabled` | `false` | Turns on Gemini match explanations; also needs `api-key`. Off means the standard explanation is always used |
| `app.ai.explanation.api-key` | none; set only in `application-local.yml` | Gemini API key. Never committed, logged or sent to the browser |
| `app.ai.explanation.base-url` | `https://generativelanguage.googleapis.com/v1beta` | Gemini API base URL |
| `app.ai.explanation.model` | `gemini-3.5-flash` | Gemini model ID; part of the explanation cache key. `gemini-2.5-flash` returns 404 for new projects |
| `app.ai.explanation.timeout` | `5s` | Connect timeout and read timeout, each; the worst-case wait is about twice this before falling back to the standard explanation. A first call on a fresh connection can take over 3 seconds |
| `app.ai.explanation.max-output-tokens` | `200` | Upper bound on Gemini's reply length |
| `app.ai.explanation.max-characters` | `600` | Longest AI explanation shown; fresh and cached text are checked, with standard fallback on rejection |
| `app.ai.explanation.daily-call-limit` | `18` | Gemini calls per day before falling back; just under the free tier's 20 requests per day per project and model. In memory and resets at Singapore midnight, while Google's limit resets on Pacific time, so they can drift apart. Usage is visible at https://ai.dev/rate-limit |
| `app.ai.explanation.daily-call-limit-per-student` | `5` | Gemini calls one student can trigger per day, so one student cannot use up the shared allowance. Cached explanations cost nothing. In memory, resetting at Singapore midnight with the shared limit |
| `app.ai.explanation.quota-zone` | `Asia/Singapore` | Time zone whose midnight resets the daily limit |
| `app.ai.explanation.rate-limit-cooldown` | `60s` | After Gemini answers 429 (too many requests), no further calls are made for this long; students get the standard explanation meanwhile |

The cache uses rounded anonymous evidence plus prompt version (`v5`) and model, separately
from the exact card evaluation ID. Two evaluations may reuse equivalent wording after each
has passed the freshness check. Cache hits are revalidated against current evidence and the
length policy; rejected cached text falls back without a provider call. Simultaneous misses
may each call Gemini; unique insertion prevents duplicate rows, not duplicate provider calls.

## Study room music (Spotify and Freesound)

| Key | Default | Purpose |
| --- | --- | --- |
| `app.ambient.api-key` | none; set only in `application-local.yml` | Freesound API key for the study rooms' Music tab. Without it a sound request returns 503 `AMBIENT_UNAVAILABLE`. Never committed, logged or sent to the browser |
| `app.ambient.base-url` | `https://freesound.org/apiv2` | Freesound API base URL |
| `app.ambient.timeout` | `5s` | Connect and read timeout for Freesound |
| `app.ambient.candidate-count` | `15` | Search results a random sound is picked from |
| `app.ambient.min-duration-seconds` / `max-duration-seconds` | `30` / `600` | Sound lengths asked for; shorter clips loop audibly, longer ones start slowly |
| `app.ambient.license` | `Creative Commons 0` | Freesound licence sounds must have. CC0 needs no credit; changing it to one that does means crediting each author on screen again |
| `app.ambient.categories` | rain, cafe, forest, ocean, fireplace, wind | Channels in the study rooms' Music tab: `id`, display `label` and the Freesound search `query` |
| `app.spotify.client-id` / `client-secret` | none; set only in `application-local.yml` | Spotify app credentials from the developer dashboard. Without both, the Premium player says Spotify sign-in is not set up. The secret is never committed, logged or sent to the browser |
| `SPOTIFY_REDIRECT_URI` / `app.spotify.redirect-uri` | `http://127.0.0.1:8080/api/spotify/callback` | Where Spotify returns after sign-in; must be registered in the Spotify dashboard. Spotify rejects `localhost` |
| `SPOTIFY_RETURN_URL` / `app.spotify.return-url` | `http://localhost:5173/study-room` | The Study Room page the browser returns to (its Music settings tab opens), with `?spotify=connected/denied/expired/failed` |
| `app.spotify.accounts-url` | `https://accounts.spotify.com` | Spotify sign-in base URL |
| `app.spotify.scopes` | `streaming`, `user-read-email`, `user-read-private` | Permissions asked for; all three are required by the Web Playback SDK |
| `app.spotify.timeout` | `5s` | Connect and read timeout for Spotify |
| `app.spotify.authorization-lifetime` | `10m` | How long a started sign-in stays valid |
| `app.spotify.token-refresh-margin` | `60s` | Renew a student's Spotify token this long before it expires |
| `app.spotify.embed-playlist-id` | `3bZTQGrvewbkJJe6UpTFKc` (the team's study playlist) | Playlist in the embedded player for students without Premium; works without app credentials |

## AI study assistant

| Key | Default | Purpose |
| --- | --- | --- |
| `app.ai.assistant.enabled` | `false` | Turns on the assistant's Gemini calls; also needs `api-key`. Off means every AI action returns 503 `AI_UNAVAILABLE` and the screen offers the manual path |
| `app.ai.assistant.api-key` | none; set only in `application-local.yml` | Gemini API key from a different Google project than E1's, so the two features do not share a quota. Never committed, logged or sent to the browser |
| `app.ai.assistant.base-url` | `https://generativelanguage.googleapis.com/v1beta` | Gemini API base URL |
| `app.ai.assistant.model` | `gemini-3.5-flash` | Gemini model ID, the same as E1 |
| `app.ai.assistant.timeout` | `90s` | Connect timeout and read timeout, each. Longer than E1's because reading a PDF and writing a roadmap or a ten-question quiz takes far longer than one sentence; a quiz from a lecture PDF can take Gemini over 30 seconds |
| `app.ai.assistant.max-output-tokens` | `4000` | Upper bound on Gemini's reply length |
| `app.ai.assistant.daily-call-limit` | `18` | Assistant calls per day before returning `AI_UNAVAILABLE`; just under the free tier's 20 requests per day per project and model. In memory and resets at midnight in `quota-zone` |
| `app.ai.assistant.daily-call-limit-per-student` | `5` | Assistant calls one student can trigger per day, so one student cannot use up the shared allowance |
| `app.ai.assistant.quota-zone` | `Asia/Singapore` | Time zone whose midnight resets the daily limits |
| `app.ai.assistant.rate-limit-cooldown` | `60s` | After Gemini answers 429 (too many requests), no further assistant calls are made for this long |
| `app.ai.assistant.study-plan.max-upload-size` | `10MB` | Most that all of one plan's files may add up to; more gets 413. Spring's `spring.servlet.multipart` limits read this value. Base64 makes attached PDFs a third larger, which stays inside Gemini's inline request limit |
| `app.ai.assistant.study-plan.max-files` | `6` | Most files for one plan, such as one week's slides each |
| `app.ai.assistant.study-plan.max-topics-characters` | `4000` | Longest pasted topics text |
| `app.ai.assistant.study-plan.max-text-file-characters` | `100000` | Most text all of one plan's `.txt` and `.md` files may hold together (about 25,000 tokens), since their text goes into the prompt; more gets 413 |
| `app.ai.assistant.study-plan.max-source-topics` | `40` | Most topics Gemini may list from the material |
| `app.ai.assistant.study-plan.max-entries-per-block` | `5` | Most entries in one week or meeting of a roadmap |
| `app.ai.assistant.study-plan.max-tasks-per-entry` | `5` | Most tasks in one roadmap entry |
| `app.ai.assistant.study-plan.max-weeks` | `16` | Most weeks a personal plan covers, so the exam can be at most this far away |
| `app.ai.assistant.study-plan.max-hours-per-week` | `40` | Most study hours per week a student can plan |
| `app.ai.assistant.study-plan.max-session-meetings` | `12` | Most upcoming meetings of a weekly study session that one session plan covers |
| `app.ai.assistant.study-plan.time-zone` | `Asia/Singapore` | Calendar that decides "today", the first day of a personal plan |
| `app.ai.assistant.quiz.question-count` | `10` | Exactly how many questions every group quiz has; stated in the prompt, and a reply with any other number is rejected |
| `app.ai.assistant.quiz.open-duration` | `15m` | How long a group quiz stays open before it reads as closed; the host can close it sooner |
| `app.ai.assistant.quiz.max-focus-characters` | `200` | Longest optional focus a student can give a quiz ("what should it ask about?"); it also titles the quiz, so it may be at most 255. The material itself (uploaded files or pasted notes) uses the `study-plan` upload limits above |
| `app.ai.assistant.chat.max-message-characters` | `500` | Longest message a student can send the study assistant |
| `app.ai.assistant.chat.matches-shown` | `3` | Buddy matches shown in an assistant reply; the rest are on Find buddies |
| `app.ai.assistant.chat.time-zone` | `Asia/Singapore` | Calendar the assistant uses for "today", to read dates such as "exam on 20 Nov" |
| `app.ai.assistant.chat.model` | `gemini-3.5-flash-lite` | Lighter Gemini model that reads assistant messages, with its own free-tier limit, so chatting never spends the quiz and study plan allowance. Clear messages such as the suggested questions are read without any AI call |
| `app.ai.assistant.chat.max-output-tokens` | `400` | Upper bound on the reply that classifies a message |
| `app.ai.assistant.chat.timeout` | `20s` | Connect and read timeout for reading a message |
| `app.ai.assistant.chat.daily-call-limit` | `200` | Assistant messages sent to the AI per day, for all students; set just under the chat model's free daily limit in Google AI Studio |
| `app.ai.assistant.chat.daily-call-limit-per-student` | `30` | Assistant messages one student can send to the AI per day |

A roadmap covers one or more weeks (personal) or upcoming meetings (session), so one week's
slides can give a plan for that week or that meeting, and several weeks' slides a roadmap to the
exam. When the material is shorter than the plan, the later weeks revise and practise earlier
topics. A student may keep several personal plans; a new one never replaces an old one. A study
session keeps one plan, covering its upcoming meetings, which whoever manages the session (the
group leader, or either buddy) creates and every participant reads; a new one replaces it.

Study material is one or more PDF, `.txt` or `.md` files, or pasted topics, never both. Each
file's type is checked from its contents as well as its extension: a `.pdf` must start with the
PDF signature and a text file must be UTF-8, otherwise 415. PDFs are attached to the Gemini
request in upload order; text goes into the prompt. Only the file names are stored.

The roadmap prompt (`prompts/study-plan-v2.txt`) states these limits and the time each week or
meeting can hold. When a week has more topics than `max-entries-per-block`, the prompt asks
Gemini to group related topics into one entry, so a one-week plan works for a full lecture deck.
Gemini's reply is then checked against the limits: every week or meeting has at least one entry,
no entry falls outside the plan, a block's minutes fit its limit and every text fits its column.
A reply that is the object wrapped in a one-item list is read as the object. Any other reply is
rejected with `AI_UNAVAILABLE`, and the backend logs which check it failed. These checks bound the
shape and size of a roadmap; they do not prove its advice is good.

## Study rooms, sessions and weather

| Key | Default | Purpose |
| --- | --- | --- |
| `ROOM_DEFAULT_FOCUS_MINUTES` / `app.study-room.default-focus-minutes` | `25` | Initial focus duration |
| `ROOM_DEFAULT_BREAK_MINUTES` / `app.study-room.default-break-minutes` | `5` | Initial break duration |
| `ROOM_MAX_FOCUS_MINUTES` / `app.study-room.max-focus-minutes` | `180` | Longest focus duration |
| `ROOM_MAX_BREAK_MINUTES` / `app.study-room.max-break-minutes` | `60` | Longest break duration |
| `ROOM_LEASE_LIFETIME` / `app.study-room.lease-lifetime` | `90s` | How long a browser stays present without a heartbeat; above the one-minute timer limit browsers apply to background tabs |
| `ROOM_POLL_INTERVAL` / `app.study-room.poll-interval` | `2s` | Shared room refresh interval |
| `ROOM_REACTION_WINDOW` / `app.study-room.reactions.window` | `8s` | How long a reaction stays in room snapshots; longer than the poll interval so every tab sees it |
| `ROOM_REACTION_BURST_LIMIT` / `ROOM_REACTION_BURST_WINDOW` | `5` / `10s` | Most reactions one student can send in each window; more return 409 |
| `app.forest.zone` | `Asia/Singapore` | Zone whose local start time picks a tree's dawn, day, dusk or night colours |
| `app.forest.light-starts` | `5/11/17/21` | Local hour dawn, daytime, dusk and night begin; a visit's start time picks its tree colours |
| `app.forest.update-queue-capacity` | `1000` | Forest updates the single forest worker may hold; beyond this one is dropped and logged |
| `app.forest.variants-per-species` | `3` | Kinds of tree the server draws from at random within each size tier; the frontend draws three per tier |
| `app.forest.tree-minutes` | `10/25/50/90/150` | Focused minutes in one room visit for a sprout, sapling, oak, tall pine and ancient blossom; shorter visits plant nothing |
| `SESSION_MAXIMUM_OCCURRENCES` / `app.study-session.maximum-occurrences` | `52` (1 to 52) | Most weekly occurrences; 1 means one-off |
| `SESSION_MAXIMUM_DURATION_HOURS` / `app.study-session.maximum-duration-hours` | `24` | Longest single occurrence |
| `WEATHER_ENABLED` / `app.weather.enabled` | `true` (`false` in tests) | Turns the campus weather provider on |
| `WEATHER_LATITUDE` / `WEATHER_LONGITUDE` | `1.297` / `103.849` | Campus coordinates sent to the provider |
| `WEATHER_LOCATION` / `app.weather.location` | `SMU campus` | Display label for those coordinates |
| `WEATHER_TIMEOUT` / `app.weather.timeout` | `4s`, at most `10s` | Provider connection and request timeout |
| `WEATHER_CACHE_LIFETIME` / `app.weather.cache-lifetime` | `15m`, at least `30s` | How long a successful reading is reused |
| `WEATHER_RETRY_INTERVAL` / `app.weather.retry-interval` | `1m`, at least `30s` | Wait after a failed request |
| `WEATHER_MAXIMUM_STALE_AGE` / `app.weather.maximum-stale-age` | `24h`, not below the cache lifetime | Oldest reading still shown |

## Demo seed and frontend

| Key | Default | Purpose |
| --- | --- | --- |
| `DEMO_SEED_ENABLED` / `app.demo-seed.enabled` | `false` | Runs the demo seeder for one startup |
| `DEMO_STUDENT_PASSWORD` / `app.demo-seed.student-password` | Required only when seeding | Password for new demo students |
| `DEMO_ADMIN_PASSWORD` / `app.demo-seed.admin-password` | Required only when seeding | Password for the new demo admin |
| `VITE_API_BASE_URL` | `/api` when unset | Backend API base URL |
| `VITE_REFRESH_INTERVAL_MS` | `30000` (`15000` to `300000`) | Background refresh for lists and details |

The test profile reads `STUDYBUDDY_TEST_DATABASE_URL`, `_USERNAME`, `_PASSWORD` and
`STUDYBUDDY_TEST_JWT_SECRET`, and the migration runner reads the standard `PG*` variables.
`scripts/test-backend.sh` sets all of them itself.
