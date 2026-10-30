# Requirement coverage

Where each requirement is implemented and tested. Backend classes are under
`backend/src/main/java/com/studybuddy/`, screens under `frontend/src/features/`, backend tests under
`backend/src/test/java/com/studybuddy/` (HTTP tests in `integration/`) and frontend tests beside
their components. Endpoints are in [API_CONTRACT.md](API_CONTRACT.md), the reasoning behind each
choice in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md), and how to run the tests in
[TESTING.md](TESTING.md).

## Scope

The course requires two extra features beyond the compulsory requirements; the team delivers them
as E1 and E2.

| Group | Contents |
| --- | --- |
| [Compulsory requirements](#compulsory-requirements) | Accounts and profiles, weekly availability, matching, match requests and contact privacy, study groups, notifications and admin management, with the help guide as core support |
| [E1 AI study assistance](#e1-ai-study-assistance) | Match explanations, Quiz Buddy quizzes, personal and session study plans and the Pixel assistant, on one validated AI layer with fallbacks, privacy rules and disclosure in [AI_USAGE.md](AI_USAGE.md) |
| [E2 collaborative study experience](#e2-collaborative-study-experience) | Group rooms with a shared Pomodoro, host roles and room reactions; dated sessions and calendar export; the Study Room tab with a personal Pomodoro and music; weather scenes; the focus forest; achievements |

Team interpretations (contact format, group-size intervals, learning style, separate deactivation
and deletion) are recorded in the
[baseline contract](DESIGN_DECISIONS.md#baseline-requirement-contract) and the
[Team C decisions](DESIGN_DECISIONS.md#accounts-relationships-and-groups-team-c).

## Compulsory requirements

### Platform and setup

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Spring Boot and React/Vite applications | `StudyBuddyApplication`, `routes.tsx`, `main.tsx` | `StudyBuddyApplicationTests` |
| Layered controllers, services, repositories and DTO assemblers | Feature packages; `AccountAssembler`, `MyProfileAssembler`, `CourseAssembler`, `ProfileViewAssembler`, `StudyGroupAssembler`, `AdminUserAssembler` | `ProfileViewAssemblerTest`, `StudyGroupAssemblerTest`, HTTP tests |
| Shared entities, enums and repositories on a migrated PostgreSQL schema | `User`, `Student`, `Course`, `AvailabilitySlot`, `StudyMode`, `StudyGoal`, `GroupSizePreference`; SQL under `backend/src/main/resources/db/migrations/` | `DatabaseBoundaryTest`, `GroupSizePreferenceTest` |
| At least 10 courses and 50 synthetic students; repeat seeding preserves data | `DemoSeedService`, `DemoStudentSeeder`, `DemoScenarioSeeder`, `DemoSessionSeeder`, `DemoQuizSeeder`, `DemoPlanSeeder`, `DemoGroupHistory`, `demo-seed/demo-data.json` | `DemoSeedTest`, `DemoDataTest` |
| Backend-only database access; secrets from configuration | RLS and revoked browser grants; `application-local.yml`; shared frontend `api` client | `DatabaseBoundaryTest`, `api.test.ts` |
| Safe errors and request bounds: framework 404/405/415/406, 413 for large JSON, fractions and NUL refused, internal faults 500 | `ApiExceptionHandler`, `InternalInvariantException`, `JsonBodySizeFilter`, `TextWithoutNulDeserializer`, `HttpProperties`, `InputRules` | `ApiExceptionHandlerTest`, `RequestRobustnessHttpTest`, `InputRulesTest` |

### Authentication and roles

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Student registration and sign-in; normalised unique email; encoded passwords | `AuthService`, `AccountCreation`, `InputRules`, `AccountAssembler` | `AuthServiceTest`, `AccountCreationTest`, `InputRulesTest`, `AccountHttpTest`, `Auth.test.tsx` |
| Public registration cannot create an administrator | `AuthService` | `AccountHttpTest` |
| JWT validation, role guards, rejection of inactive, deleted or revoked accounts | `JwtTokenService`, `JwtAuthenticationFilter`, `SecurityConfiguration`, `AccountAccess` | `JwtTokenServiceTest`, `AccountAccessTest`, `AccountHttpTest`, `SignOutHttpTest` |
| Sign-in records usage without losing concurrent edits | `AuthService` | `LoginConcurrencyTest` |
| Sign-in throttling per email, with `Retry-After` | `LoginThrottle`, `TooManyLoginAttemptsException` | `LoginThrottleTest`, `LoginThrottleHttpTest` |

### Own profile and study preferences

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Read and update own profile with field errors | `ProfileService`, `UpdateProfileRequest`, `MyProfileAssembler`, `ProfileProperties`; `EditProfilePage`, `ProfileEditor` | `ProfileServiceTest`, `EditProfilePage.test.tsx` |
| Courses, optional buddy target, mode, goals and categorical group size | `CourseService`, `Student`, `GroupSizePreference`; `StudyPreferencesFields` | `ProfileServiceTest`, `GroupSizePreferenceTest` |
| Profile writes serialised with weekly replacements | `ProfileService` | `ProfileConcurrencyTest` |
| Singapore or international contact number, normalised on every write | `InputRules`; frontend `contactNumber` helper | `InputRulesTest`, `ContactHttpTest`, `contactNumber.test.ts`, `ProfileContact.test.tsx` |
| Learning style through the defined study preferences | Team decision; no separate field | See [baseline contract](DESIGN_DECISIONS.md#baseline-requirement-contract) |

### Weekly availability

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Replace or clear a whole week; whole-minute, same-day slots; at most `max-weekly-slots` blocks | `ProfileService`, `AvailabilityRules`, `AvailabilityProperties`, `AvailabilitySlot`; `WeeklySlotEditor`, `WeeklySchedule` | `AvailabilityRulesTest`, `ProfileServiceTest`, `RequestRobustnessHttpTest`, `WeeklySlotEditor.test.tsx` |
| Own schedule rejects overlap; group schedules may overlap | `AvailabilityRules`, `StudyGroupService` | `AvailabilityRulesTest`, `GroupHttpTest` |
| Concurrent replacements never mix two weeks | `ProfileService` | `ProfileConcurrencyTest` |
| Invalid time text blocks saving and triggers exit protection | `WeeklySlotEditor` | `WeeklySlotEditor.test.tsx`, `EditProfilePage.test.tsx`, `GroupFormDrafts.test.tsx`, `GroupFormNavigation.test.tsx` |

### Shared shell and Home

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Guarded navigation: a menu bar with notifications and help, and a sidebar grouped into Connect, Study and You (with Achievements); shared retro components | `AppShell`, `MenuBar`, `NavSection`, `TabBar`, `RetroWindow`, `WindowPage`, `Button`, `Dialog`, `StatePanel`, `routes.tsx` | `AppShell.test.tsx`, `Dialog.test.tsx`, `DialogDrafts.test.tsx`, `ActionNotice.test.tsx`, `FormPolish.test.tsx` |
| Home from real profile, activity, request, group, notification and matching data | `HomePage`, `nextStep`, `profileProgress` | `HomePage.test.tsx`, `nextStep.test.ts`, `profileProgress.test.ts`, `ActivityFeed.test.tsx`, `homeTime.test.ts` |

### Matching algorithm and scoring

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Five modular 0-1 scorers with configurable weights | `CriterionScorer`, `CourseScorer`, `AvailabilityOverlapScorer`, `StudyModeScorer`, `StudyGoalScorer`, `GroupSizeScorer`, `WeightedMatchScorer` | `CriterionScorersTest`, `WeightedMatchScorerTest` |
| Bounded total, breakdown, unknown criteria and quality | `MatchScore`, `CriterionResult`, `ScoringRules`, `MatchingWeights`, `QualityThresholds` | `MatchScoreTest`, `MatchingWeightsTest`, `QualityThresholdsTest` |
| Balanced, availability-first and course-first strategies | `MatchingStrategy`, `PriorityTierStrategy`, `PriorityTiers`, `CourseFirstStrategy` | `MatchingStrategyRankingTest`, `PriorityTiersTest` |
| Appendix A worked example (2 h overlap, 0.85 Strong) | Whole engine | `AppendixAMatchingTest` |
| Weekly overlap maths | `TimeSlot`, `WeeklyAvailability` | `TimeSlotTest` |
| Deterministic explanation text | `MatchExplainer`, `TemplateMatchExplainer`, `MatchResultAssembler` | `TemplateMatchExplainerTest` |

### Find matches and matching API

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Course or goal search with filters | `MatchController`, `MatchService`, `MatchSearchCriteria`, `MatchSearchFilter` | `MatchSearchFilterTest`, `MatchServiceTest`, `MatchingHttpTest` |
| Exclude self, inactive accounts and current buddies; show pending state | `MatchService`, `MatchRelationships` | `MatchServiceTest`, `MatchingHttpTest` |
| Sort options, stable ties and bounded results | `MatchSort`, `MatchService`, `MatchingProperties` | `MatchServiceTest`, `MatchingPropertiesTest` |
| Privacy-safe result DTO | `MatchResultDto`, `MatchSearchResultDto`, `MatchResultAssembler` | `MatchingHttpTest` |
| Constant query count per search | `StudentRepository`, `AvailabilitySlotRepository` | `MatchingQueryCountTest` |

### Matching page

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Results with quality, score bars, shared hours and heatmap | `FindBuddiesPage`, `FindBuddiesPanel`, `MatchCard`, `ScoreBreakdown`, `SharedHoursHeatmap`, `matchPresentation` | `FindBuddiesPage.test.tsx`, `SharedHoursHeatmap.test.tsx`, `matchPresentation.test.ts` |
| Filters, reset and sort controls | `MatchFilters`, `matchSearchForm` | `FindBuddiesPage.test.tsx` |
| Send a request from a card; pending and failed states | `MatchCard`, `SendRequestDialog` | `FindBuddiesPage.test.tsx`, `BuddyHttpTest` |

### Admin matching configuration

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Choose strategy, enable criteria, edit weights and minimum score | `AdminMatchingConfigController`, `MatchingConfigService`, `MatchingProperties`; `MatchingSettingsPage`, `matchingSettingsForm` | `MatchingConfigServiceTest`, `MatchingPropertiesTest`, `MatchingSettingsPage.test.tsx`, `matchingSettingsForm.test.ts` |
| Validation: one criterion enabled, weights total 100% | `MatchingConfigService`, `MatchingWeights` | `MatchingWeightsTest`, `MatchingHttpTest` |
| Persisted overrides, restore defaults, versioned saves, admin only | `MatchingConfigService` | `MatchingConfigServiceTest`, `MatchingHttpTest` |

### Match requests and connections

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Send a request with optional message and context | `MatchRequestService`, `MatchRequestContext`; `StudentProfilePage`, `SendRequestDialog` | `MatchRequestServiceTest`, `BuddyHttpTest` |
| Recipient accepts or declines | `MatchRequestService`, `MatchRequest`; `ConnectionsPage` | `MatchRequestTest`, `BuddyHttpTest`, `ConnectionsPage.test.tsx` |
| Sender withdraws a pending request | `MatchRequestService`, `MatchRequest` | `LifecycleHttpTest`, `ConnectionsPage.test.tsx` |
| Reject self, duplicate, reverse and already-connected requests | `MatchRequestService` | `MatchRequestServiceTest`, `BuddyHttpTest` |
| Accepting creates one connection; either student can end it | `Connection`, `ConnectionService` | `ConnectionTest`, `ConnectionServiceTest`, `BuddyHttpTest` |

### Profiles and contact privacy

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Contact visible only to self and connected students | `ProfileViewService`, `ProfileViewAssembler`, `PublicProfileDto`, `ConnectedProfileDto` | `ProfileViewAssemblerTest`, `ProfileViewServiceTest`, `BuddyHttpTest`, `StudentProfilePage.test.tsx` |
| Group membership never reveals contact | `StudyGroupAssembler` | `StudyGroupAssemblerTest`, `GroupHttpTest` |
| Disconnecting removes access immediately | `ProfileViewService` | `BuddyHttpTest` |

### Study groups and membership

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Create a course group with goals, mode, capacity and schedule | `StudyGroupService`, `StudyGroup`, `GroupAvailabilitySlot`; `GroupFormPage` | `StudyGroupServiceTest`, `StudyGroupTest`, `GroupHttpTest`, `GroupFormDrafts.test.tsx` |
| Browse, filter and view groups | `StudyGroupService`, `StudyGroupFilter`; `GroupsPage`, `GroupDetailPage` | `StudyGroupFilterTest`, `GroupsBrowsing.test.tsx` |
| Apply; leader approves or rejects | `GroupJoinRequestService`, `GroupJoinRequest`; `ApplyGroupDialog`, `GroupManagePage` | `GroupJoinRequestServiceTest`, `GroupJoinRequestTest`, `Groups.test.tsx` |
| Withdraw an application; leave a group | `GroupJoinRequestService`, `StudyGroupService` | `LifecycleHttpTest`, `Groups.test.tsx` |
| Capacity includes the leader, is never exceeded and is at most `max-capacity` | `StudyGroup`, `GroupMembership`, `StudyGroupService`, `StudyGroupProperties` | `StudyGroupTest`, `StudyGroupServiceTest`, `GroupHttpTest`, `RequestRobustnessHttpTest` |
| Leader edits, removes members and closes the group | `StudyGroupService`, `GroupClosure` | `GroupHttpTest` |

### Notifications

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Notify on request, connection and group events in the same transaction | `NotificationService` | `NotificationServiceTest`, `BuddyHttpTest`, `GroupHttpTest` |
| List, filter, mark read, unread badge; own notifications only | `NotificationService`, `NotificationFilter`; `NotificationsPage`, `AppShell` | `NotificationsPage.test.tsx`, `BuddyHttpTest` |
| Links open the right request view | `NotificationRequestDirection` | `NotificationDirectionTest` |

### Admin accounts

| Requirement | Main classes | Tests |
| --- | --- | --- |
| List, search, filter and view accounts | `AdminUserService`, `AdminUserFilter`; `AdminUsersPage`, `AdminUserDetailPage` | `AdminUserFilterTest`, `AdminUsers.test.tsx`, `AccountHttpTest` |
| Create and edit students and admins; contact write-only | `AdminUserService`, `AccountCreation`, `AdminUserAssembler`; `AdminUserFormPage` | `AccountHttpTest`, `AdminUserNavigation.test.tsx` |
| Basic usage information | `UserUsageCounter` | `AdminUserServiceTest`, `AccountHttpTest` |
| Deactivate and reactivate | `StudentDeactivation`; `DeactivateUserDialog` | `AccountHttpTest`, `GroupHttpTest` |
| Permanently delete | `StudentDeletion`, `GroupClosure` | `AccountHttpTest` |
| Admins cannot remove themselves | `AdminUserService` | `AdminUserServiceTest` |

### Help guide (core support)

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Next step and getting-started checklist from current records | `HelpPage`, `helpNextStep`, `startSteps`, `StartChecklist` | `HelpPage.test.tsx`, `helpNextStep.test.ts` |
| Topic guides and searchable FAQ | `helpContent`, `ExplainerCard`, `FaqSection`, `FaqItem` | `HelpPage.test.tsx` |

## E1 AI study assistance

### Match explanations

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Explain a current card on request without changing the score | `MatchExplanationController`, `MatchExplanationService`, `MatchEvaluationFingerprint`; `MatchExplanationPanel` | `MatchExplanationServiceTest`, `MatchExplanationHttpTest`, `MatchExplanationPanel.test.tsx` |
| Anonymous allowlisted evidence | `ExplanationEvidenceFactory`, `ExplanationVocabulary`, `ExplanationPromptBuilder` | `ExplanationEvidenceFactoryTest`, `ExplanationVocabularyTest`, `ExplanationPromptBuilderTest` |
| Output checks | `ExplanationOutputValidator` | `ExplanationOutputValidatorTest` |
| Gemini call with fallback to standard text | `AiExplanationConfiguration`, `GeminiExplanationProvider`, `AiExplanationWriter` | `GeminiExplanationProviderTest`, `AiExplanationWriterTest` |
| E1's own key, model and daily quota, shared and per student | `AiExplanationProperties`, `GeminiSettings`, `AiQuota` | `AiExplanationPropertiesTest`, `AiQuotaTest` |
| Cache keyed by anonymous evidence | `ExplanationHasher`, `ExplanationCache` | `ExplanationHasherTest`, `MatchExplanationHttpTest` |

### Quizzes

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Personal and group quizzes from notes | `QuizController`, `QuizCreationService`, `QuizWriter`, `QuizPromptBuilder`; `QuizBotLauncher`, `QuizBotWindow`, `QuizBotChat` | `QuizCreationServiceTest`, `QuizPromptBuilderTest`, `QuizHttpTest`, `QuizBotWindow.test.tsx` |
| Quiz for one of the group's upcoming sessions | `QuizService`, `Quiz`; `QuizBotChat`, `QuizBotWindow` | `QuizHttpTest`, `QuizBotWindow.test.tsx` |
| Reply validation | `QuizOutputValidator` | `QuizOutputValidatorTest` |
| Who may start, take, close and review | `QuizAccess`, `QuizService` | `QuizAccessTest`, `QuizHttpTest` |
| Timed attempts and marking | `Quiz`, `QuizAttempt`, `QuizMarker`; `TakeQuizDialog` | `QuizTest`, `QuizAttemptTest`, `QuizMarkerTest` |
| Leaderboard and review | `QuizLeaderboard`; `QuizLeaderboardDialog`, `QuizReviewDialog` | `QuizLeaderboardTest`, `QuizHttpTest` |

### Study plans

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Personal week-by-week roadmap from notes | `StudyPlanController`, `StudyPlanCreationService`, `StudyPlanService`, `StudyPlan`, `StudyPlanEntry`; `StudyPlanPage`, `CreateStudyPlanPage`, `StudyPlanDetailPage` | `StudyPlanHttpTest`, `StudyPlanTest`, `StudyPlan.test.tsx` |
| Weeks up to the exam | `StudyPlanCalendar`, `RoadmapFrame` | `StudyPlanCalendarTest`, `RoadmapFrameTest` |
| Upload type and size checks | `StudyMaterialReader` | `StudyMaterialReaderTest` |
| Prompt, Gemini call and reply validation | `StudyPlanPromptBuilder`, `RoadmapWriter`, `StudyPlanOutputValidator` | `StudyPlanPromptBuilderTest`, `RoadmapWriterTest`, `StudyPlanOutputValidatorTest`, `StudyPlanPromptLiveTest` (opt-in) |
| Session roadmap for a study session's upcoming meetings | `SessionStudyPlanController`, `SessionMeetingFrames`, `StudyPlanCreationService`; `SessionRoadmapPanel`, `SessionPlanForm` | `SessionMeetingFramesTest`, `SessionStudyPlanHttpTest`, `SessionRoadmapPanel.test.tsx` |
| Saving after the AI call: ended meetings dropped, edited sessions refused | `StudyPlanService`, `SessionMeetingFrames`, `RoadmapDraft` | `StudyPlanSessionSaveTest`, `SessionMeetingFramesTest` |
| Owner-only access; session plans follow session access | `StudyPlanAccess` | `StudyPlanHttpTest`, `SessionStudyPlanHttpTest` |

### Pixel study assistant

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Floating chat for students | `AssistantController`, `AssistantService`; `AssistantLauncher`, `AssistantPanel`, `AssistantReplyView` | `AssistantServiceTest`, `AssistantHttpTest`, `AssistantLauncher.test.tsx` |
| Answer common questions without an AI call | `QuickIntentReader` | `QuickIntentReaderTest` |
| AI intent reading with its own model and allowance | `IntentParser`, `AssistantAi`, `AssistantConfiguration`, `AssistantPromptBuilder` | `AssistantPromptBuilderTest`, `AssistantServiceTest` |
| Reply validation | `AssistantOutputValidator` | `AssistantOutputValidatorTest` |
| To-do, navigation, buddies, meeting times, quiz and plan hand-offs | `MyToDoIntent`, `NavigateIntent`, `FindBuddiesIntent`, `WhenToMeetIntent`, `QuizMeIntent`, `PlanStudyIntent`, `AppPage` | `NavigateAndToDoIntentsTest`, `AssistantIntentsTest` |
| Plan a session and say what its next meeting covers | `PlanSessionIntent`, `SessionTopicsIntent`, `AssistantSessions` | `SessionIntentsTest` |

### Shared AI layer

| Requirement | Main classes | Tests |
| --- | --- | --- |
| One Gemini client class, an instance and quota per feature | `AiClient`, `GeminiClient`, `GeminiSettings`, `AiQuota`, `AiAttachment`, `AiAssistantConfiguration`, `AiAssistantProperties` | `GeminiClientTest`, `AiQuotaTest`, `AiAttachmentTest`, `AiAssistantPropertiesTest`, `GeminiClientLiveTest` (opt-in) |
| Versioned prompt templates filled in one pass | `PromptTemplate`, `StudyMaterial` | `PromptTemplateTest` |

## E2 collaborative study experience

### Group rooms, sessions and weather

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Private group room with presence leases and capacity | `StudyRoomService`, `RoomAccess`, `RoomPresence`; `StudyRoomPage` | `StudyRoomHttpTest`, `StudyRoomPage.test.tsx` |
| Host handover and room start invitations | `StudyRoomService`, `RoomStartNotifications` | `StudyRoomDepartureTest`, `RoomStartNotificationsTest` |
| Removal, closure and deactivation clear leases and roles and end open forest visits | `RoomLifecycle` | `RoomLifecycleTest`, `StudyRoomHttpTest` |
| Shared Pomodoro | `PomodoroTimer`; `RoomTimerPanel` | `PomodoroTimerTest`, `RoomTimerPanel.test.tsx`, `timer.test.ts`, `timerPing.test.ts` |
| Personal Pomodoro | `StudyRoomsPage`, `PersonalPomodoro` | `PersonalPomodoro.test.tsx`, `StudyRoomsPage.test.tsx` |
| Dated group and buddy sessions with recurrence; 404 for students with no tie to the session | `StudySessionService`, `SessionAccess`, `StudySession`, `SessionSchedule`, `SessionSettings`; `SessionsPage`, `SessionFormPage`, `SessionDetailPage` | `SessionScheduleTest`, `StudySessionHttpTest`, `Sessions.test.tsx` |
| Mutual availability suggestions | `SessionAvailability` | `StudySessionHttpTest` |
| Session lifecycle and notifications | `SessionEvents`, `StudySessionLifecycle` | `StudySessionHttpTest` |
| Calendar export | `SessionCalendar` | `SessionCalendarTest`, `StudySessionHttpTest` |
| Campus weather and pixel themes | `WeatherService`, `WeatherClient`, `OpenMeteoClient`; `RoomWeatherPanel` | `WeatherServiceTest`, `RoomWeatherPanel.test.tsx` |

### Room reactions

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Send one of six fixed reactions while present in the room; burst limit per student, counted under the sender's lock | `StudyRoomController`, `RoomReactionService`, `RoomReactionType`, `RoomReactionSettings`; `ReactionTray` | `RoomReactionServiceTest`, `RoomReactionsAndForestHttpTest`, `ReactionLayer.test.tsx` |
| Every member's room snapshot lists recent reactions; old rows are pruned | `RoomReactionService`, `RoomReaction`, `RoomReactionRepository`, `RoomReactionDto`, `StudyRoomService` | `RoomReactionsAndForestHttpTest` |
| Original pixel sprites float once per reaction, the sender's at once | `ReactionLayer`, `ReactionSprite`, `reactionSprites` | `ReactionLayer.test.tsx` |

### Focus forest

| Requirement | Main classes | Tests |
| --- | --- | --- |
| One tree per continuous room visit; focus time measured on the shared timer | `ForestService`, `FocusCredit`, `FocusVisit`, `FocusVisitRepository`; room events `RoomVisitTicked`, `RoomVisitEnded` | `FocusCreditTest`, `ForestServiceTest`, `RoomReactionsAndForestHttpTest` |
| Room updates reach the forest after commit on its own worker | `ForestVisitListener`, `ForestConfiguration` | `RoomReactionsAndForestHttpTest` |
| Tiers from configured minutes; colours from the visit's local start time; random kind and plot | `TreeGrowth`, `TreeSpecies`, `TreeLight`, `TreeLottery`, `ForestProperties` | `TreeGrowthTest`, `TreeLotteryTest` |
| Own forest page and live growing tree in the room | `ForestController`, `ForestAssembler`; `ForestPage`, `Meadow3D`, `meadowLayout`, `GrowingTree` | `RoomReactionsAndForestHttpTest`, `ForestPage.test.tsx`, `meadowLayout.test.ts`, `GrowingTree.test.tsx` |
| Seeded demo forest | `DemoForestSeeder` | `DemoSeedTest` |

### Music

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Personal Music tab and mini player in both rooms | `MusicPlayer`, `MusicProvider`, `NowPlayingMini` | `StudyRoomPage.test.tsx`, `StudyRoomsPage.test.tsx` |
| Spotify sign-in, in-memory tokens and in-browser playback | `SpotifyController`, `SpotifyConnectionService`, `SpotifyAccountsClient`, `SpotifyAuthorizationStore`, `SpotifyTokenStore`; `SpotifyPanel`, `SpotifyEmbed`, `SpotifyPremiumPlayer` | `SpotifyConnectionServiceTest`, `SpotifyAuthorizationStoreTest`, `SpotifyResponseParsingTest` |
| Freesound CC0 ambient sounds and remix | `AmbientController`, `AmbientSoundService`, `FreesoundClient`; `AmbientChannel`, `useAmbientMixer`, `remixPlan` | `AmbientSoundServiceTest`, `FreesoundClientTest`, `useAmbientMixer.test.ts`, `remixPlan.test.ts` |

### Achievements

| Requirement | Main classes | Tests |
| --- | --- | --- |
| Seven private badges derived from existing records, including Forester for planted trees | `AchievementController`, `AchievementService`, `BadgeDefinition`, `AchievementAssembler` | `AchievementServiceTest`, `AchievementAssemblerTest`, `AchievementHttpTest` |
| Tiers, points and level | `BadgeEvaluator`, `AchievementProperties` | `BadgeEvaluatorTest`, `AchievementPropertiesTest` |
| Earned tiers are kept when counts fall | `EarnedBadgeRecorder`, `EarnedBadge`, `EarnedBadgeRepository` | `EarnedBadgeRecorderTest`, `AchievementServiceTest`, `AchievementHttpTest` |
| Planned-session weekly streak | `WeeklyStreakCalculator`, `AchievementSessionRepository` | `WeeklyStreakCalculatorTest`, `AchievementHttpTest` |
| Achievements page, reached from You in the sidebar, and own-profile summary | `AchievementsPage`, `StudyGrove`, `OwnAchievementSummary`, `AppShell` | `AchievementsPage.test.tsx`, `StudentProfilePage.test.tsx`, `AppShell.test.tsx` |
