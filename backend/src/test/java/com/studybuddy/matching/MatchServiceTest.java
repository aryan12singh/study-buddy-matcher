package com.studybuddy.matching;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.matching.config.MatchingConfigService;
import com.studybuddy.matching.config.MatchingProperties;
import com.studybuddy.matching.config.MatchingConfigSnapshot;
import com.studybuddy.matching.scoring.AvailabilityOverlapScorer;
import com.studybuddy.matching.scoring.CourseScorer;
import com.studybuddy.matching.scoring.GroupSizeScorer;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.scoring.StudyGoalScorer;
import com.studybuddy.matching.scoring.StudyModeScorer;
import com.studybuddy.matching.scoring.WeightedMatchScorer;
import com.studybuddy.matching.strategy.AvailabilityFirstStrategy;
import com.studybuddy.matching.strategy.BalancedStrategy;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.matchrequest.MatchRequest;
import com.studybuddy.matchrequest.MatchRequestOrigin;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.AvailabilitySlot;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.ProfileProperties;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentNotFoundException;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.student;
import static com.studybuddy.matching.MatchingFixtures.evenWeights;
import static java.time.DayOfWeek.MONDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchServiceTest {

    private static final long ME = 1L;

    @Mock private AccountAccess access;
    @Mock private StudentRepository students;
    @Mock private AvailabilitySlotRepository availabilitySlots;
    @Mock private CourseRepository courses;
    @Mock private MatchingConfigService config;
    @Mock private ConnectionRepository connections;
    @Mock private MatchRequestRepository requests;

    private MatchService service;
    private Student me;
    private Student strong;
    private Student weak;

    @BeforeEach
    void setUp() {
        MatchingProperties properties = MatchingFixtures.properties();
        WeightedMatchScorer scorer = new WeightedMatchScorer(List.of(new CourseScorer(MatchingFixtures.rules()), new AvailabilityOverlapScorer(MatchingFixtures.rules()),
                new StudyModeScorer(), new StudyGoalScorer(), new GroupSizeScorer()), properties.quality(), properties.scoring());
        service = new MatchService(access, students, availabilitySlots, courses, config, properties,
                new MatchSearchFilter(new ProfileProperties(5, 8), properties.scoring()), scorer,
                new MatchResultAssembler(properties.scoring(), new TemplateMatchExplainer()),
                new MatchRelationships(connections, requests));

        me = student(ME, "Priya");
        me.setPreferredStudyMode(StudyMode.ONLINE);
        me.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        strong = student(2, "Jamie");
        strong.setTargetCourse(course(4, "IS442"));
        strong.setPreferredStudyMode(StudyMode.EITHER);
        strong.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        weak = student(3, "Alex");
        weak.getCoursesTaken().add(course(4, "IS442"));
        weak.setPreferredStudyMode(StudyMode.IN_PERSON);
        weak.getStudyGoals().add(StudyGoal.PROBLEM_SOLVING);

        when(access.requireStudent(ME)).thenReturn(me);
        when(access.eligibleStudentIds()).thenReturn(List.of(1L, 2L, 3L));
        when(students.findWithMatchingDetailsByIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(weak, me, strong));
        when(courses.existsById(4L)).thenReturn(true);
        when(availabilitySlots.findByStudentIdIn(any())).thenReturn(List.of(
                new AvailabilitySlot(me, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new AvailabilitySlot(strong, MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new AvailabilitySlot(weak, MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))));
        when(config.snapshot()).thenReturn(new MatchingConfigSnapshot(new BalancedStrategy(),
                new MatchingWeights(evenWeights()), EnumSet.allOf(MatchingCriterion.class), 0, 0));
    }

    private static MatchSearchCriteria courseSearch(MatchQuality minQuality) {
        return new MatchSearchCriteria(4L, null, null, null, null, minQuality);
    }

    @Test
    void ranksOtherStudentsBestFirstAndNeverIncludesTheSearcher() {
        List<MatchResultDto> results = service.search(courseSearch(null), ME).matches();

        assertEquals(List.of(2L, 3L), results.stream().map(MatchResultDto::studentId).toList());
        assertEquals(MatchQuality.STRONG, results.get(0).quality());
        assertEquals(2.0, results.get(0).sharedHoursPerWeek());
        assertEquals(1.0, results.get(1).sharedHoursPerWeek());
    }

    @Test
    void reportsTheAdminsActiveStrategy() {
        when(config.snapshot()).thenReturn(new MatchingConfigSnapshot(new AvailabilityFirstStrategy(MatchingFixtures.tiers()),
                new MatchingWeights(evenWeights()), EnumSet.allOf(MatchingCriterion.class), 0, 3));

        assertEquals(MatchingStrategyType.AVAILABILITY_FIRST, service.search(courseSearch(null), ME).strategy());
    }

    @Test
    void showsWhereTheSearcherAlreadyStandsWithEachStudent() {
        Student pending = student(4, "Sam");
        Student connected = student(5, "Kai");
        pending.getCoursesTaken().add(course(4, "IS442"));
        connected.getCoursesTaken().add(course(4, "IS442"));
        when(access.eligibleStudentIds()).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(students.findWithMatchingDetailsByIdIn(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(List.of(weak, me, strong, pending, connected));
        when(requests.findBySenderIdAndStatus(ME, MatchRequestStatus.PENDING))
                .thenReturn(List.of(new MatchRequest(me, strong, null, MatchRequestOrigin.MATCHING, null, null)));
        when(requests.findByReceiverIdAndStatus(ME, MatchRequestStatus.PENDING))
                .thenReturn(List.of(new MatchRequest(pending, me, null, MatchRequestOrigin.PROFILE, null, null)));
        when(connections.findActiveByStudentId(ME)).thenReturn(List.of(new Connection(connected, me)));

        Map<Long, RelationshipState> states = service.search(courseSearch(null), ME)
                .matches().stream().collect(Collectors.toMap(MatchResultDto::studentId, MatchResultDto::relationship));

        assertEquals(Map.of(2L, RelationshipState.OUTGOING_PENDING, 3L, RelationshipState.STRANGER,
                4L, RelationshipState.INCOMING_PENDING), states);
    }

    @Test
    void minimumQualityHidesWeakerMatches() {
        List<MatchResultDto> results = service.search(courseSearch(MatchQuality.STRONG), ME).matches();

        assertEquals(List.of(2L), results.stream().map(MatchResultDto::studentId).toList());
    }

    @Test
    void filtersNarrowTheResults() {
        MatchSearchCriteria online = new MatchSearchCriteria(4L, null, StudyMode.ONLINE, null, null, null);

        assertEquals(List.of(2L), service.search(online, ME).matches().stream().map(MatchResultDto::studentId).toList());
    }

    @Test
    void returnsAnEmptyListWhenNobodyFits() {
        MatchSearchCriteria planning = new MatchSearchCriteria(null, StudyGoal.PROJECT_DISCUSSION, null, null, null, null);

        assertTrue(service.search(planning, ME).matches().isEmpty());
    }

    @Test
    void searchWithoutCourseOrGoalIsRejected() {
        assertThrows(InvalidMatchSearchException.class,
                () -> service.search(new MatchSearchCriteria(null, null, null, null, null, null), ME));
    }

    @Test
    void resultsCarryTheBreakdownSharedSlotsAndPreferences() {
        MatchResultDto jamie = service.search(courseSearch(null), ME).matches().get(0);

        assertEquals(List.of(MatchingCriterion.values()), jamie.breakdown().stream().map(CriterionScoreDto::criterion).toList());
        assertEquals(1.0, jamie.breakdown().get(0).score());
        assertEquals(List.of(new SharedSlotDto(MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))), jamie.sharedSlots());
        assertEquals(StudyMode.EITHER, jamie.studyMode());
        assertEquals(List.of(StudyGoal.EXAM_PREPARATION), jamie.studyGoals());
    }

    @Test
    void rejectsSharedHoursBelowOne() {
        assertThrows(InvalidMatchSearchException.class,
                () -> service.search(new MatchSearchCriteria(4L, null, null, null, 0, null), ME));
    }

    @Test
    void rejectsAnUnknownCourse() {
        assertThrows(CourseNotFoundException.class, () -> service.search(new MatchSearchCriteria(99L, null, null, null, null, null), ME));
    }

    @Test
    void limitAndConfigurationMetadataAgreeWithUnboundedCount() {
        var result = service.search(new MatchSearchCriteria(4L, null, null, null, null, null, MatchSort.BEST_MATCH, 1), ME);
        assertEquals(2, result.totalMatches());
        assertEquals(1, result.limit());
        assertEquals(1, result.matches().size());
        assertEquals(MatchSort.BEST_MATCH, result.sort());
        assertEquals(0, result.configurationVersion());
        assertThrows(InvalidMatchSearchException.class, () -> service.search(new MatchSearchCriteria(4L, null, null, null, null, null, null, 0), ME));
        assertThrows(InvalidMatchSearchException.class, () -> service.search(new MatchSearchCriteria(4L, null, null, null, null, null, null, 51), ME));
    }

    @Test
    void alternativeSortsUseSharedHoursOrSameYearBeforeStrategy() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.replaceAll((criterion, value) -> criterion == MatchingCriterion.COURSE ? 1.0 : 0.0);
        when(config.snapshot()).thenReturn(new MatchingConfigSnapshot(new BalancedStrategy(), new MatchingWeights(values),
                EnumSet.allOf(MatchingCriterion.class), 0, 4));
        strong.setYearOfStudy(3);
        weak.setYearOfStudy(2);
        when(availabilitySlots.findByStudentIdIn(any())).thenReturn(List.of(
                new AvailabilitySlot(me, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new AvailabilitySlot(strong, MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new AvailabilitySlot(weak, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        assertEquals(List.of(2L, 3L), service.search(courseSearch(null), ME).matches().stream().map(MatchResultDto::studentId).toList());
        for (MatchSort sort : List.of(MatchSort.MOST_SHARED_HOURS, MatchSort.SAME_YEAR_FIRST)) {
            assertEquals(List.of(3L, 2L), service.search(new MatchSearchCriteria(4L, null, null, null, null, null, sort, null), ME)
                    .matches().stream().map(MatchResultDto::studentId).toList());
        }
    }

    @Test
    void totalExplanationAndDisabledBreakdownUseExactlyTheSameSettings() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.replaceAll((criterion, value) -> criterion == MatchingCriterion.COURSE ? 1.0 : 0.0);
        when(config.snapshot()).thenReturn(new MatchingConfigSnapshot(new BalancedStrategy(), new MatchingWeights(values),
                java.util.Set.of(MatchingCriterion.COURSE), 0.75, 5));
        var result = service.search(courseSearch(null), ME);
        assertEquals(1, result.matches().size());
        var card = result.matches().getFirst();
        assertEquals(1.0, card.totalScore());
        assertEquals(4L, card.relevantCourse().id());
        assertTrue(card.explanation().contains("course 100.0%"));
        assertTrue(card.explanation().contains("not a success probability"));
        assertTrue(card.breakdown().getFirst().enabled());
        assertEquals(0, card.breakdown().get(1).score());
        assertTrue(!card.breakdown().get(1).enabled());
    }

    @Test
    void equalScoresTieOnMoreSharedHoursBeforeName() {
        // Both want a buddy for IS442 and only course is enabled, so their scores are equal.
        weak.setTargetCourse(strong.getTargetCourse());
        Map<MatchingCriterion, Double> courseOnly = evenWeights();
        courseOnly.replaceAll((criterion, weight) -> criterion == MatchingCriterion.COURSE ? 1.0 : 0.0);
        when(config.snapshot()).thenReturn(new MatchingConfigSnapshot(new BalancedStrategy(), new MatchingWeights(courseOnly),
                EnumSet.of(MatchingCriterion.COURSE), 0, 0));

        List<MatchResultDto> results = service.search(courseSearch(null), ME).matches();

        // Jamie shares 2 hours and Alex 1, so Jamie comes first although Alex sorts first by name.
        assertEquals(List.of(2L, 3L), results.stream().map(MatchResultDto::studentId).toList());
        assertEquals(results.get(0).totalScore(), results.get(1).totalScore());
    }

    @Test
    void equalScoresAndNamesTieOnStudentIdForEverySort() {
        weak.setName(strong.getName().toUpperCase(java.util.Locale.ROOT));
        weak.setTargetCourse(strong.getTargetCourse());
        weak.setPreferredStudyMode(strong.getPreferredStudyMode());
        weak.getStudyGoals().clear(); weak.getStudyGoals().addAll(strong.getStudyGoals());
        when(availabilitySlots.findByStudentIdIn(any())).thenReturn(List.of());
        for (MatchSort sort : MatchSort.values()) {
            assertEquals(List.of(2L, 3L), service.search(new MatchSearchCriteria(4L, null, null, null, null, null, sort, null), ME)
                    .matches().stream().map(MatchResultDto::studentId).toList());
        }
    }

    @Test
    void onlyStudentsCanSearch() {
        when(access.requireStudent(ME)).thenThrow(new ForbiddenActionException("A student account is required"));

        assertThrows(ForbiddenActionException.class, () -> service.search(courseSearch(null), ME));
        verifyNoInteractions(students);
    }

    @Test
    void evaluateScoresExactlyAsSearchDoes() {
        when(access.eligibleStudent(2L)).thenReturn(strong);
        MatchResultDto fromSearch = service.search(courseSearch(null), ME).matches().stream()
                .filter(result -> result.studentId() == 2L).findFirst().orElseThrow();

        MatchEvaluation evaluation = service.evaluate(courseSearch(null), ME, 2L);

        assertEquals(fromSearch, evaluation.result());
        assertEquals(MatchingStrategyType.BALANCED, evaluation.strategy());
        assertEquals(StudyMode.ONLINE, evaluation.searcherStudyMode());
        assertEquals(java.util.Set.of(StudyGoal.EXAM_PREPARATION), evaluation.searcherStudyGoals());
        assertEquals(GroupSizePreference.EITHER, evaluation.candidateGroupSize());
    }

    @Test
    void evaluateRejectsTheSearcherThemself() {
        assertThrows(StudentNotFoundException.class, () -> service.evaluate(courseSearch(null), ME, ME));
    }

    @Test
    void evaluateRejectsAnInvalidSearchBeforeLookingUpTheCandidate() {
        assertThrows(InvalidMatchSearchException.class,
                () -> service.evaluate(new MatchSearchCriteria(null, null, null, null, null, null), ME, 2L));
    }

    @Test
    void evaluatePassesOnIneligibleCandidateErrors() {
        when(access.eligibleStudent(9L)).thenThrow(new StudentNotFoundException(9L));
        assertThrows(StudentNotFoundException.class, () -> service.evaluate(courseSearch(null), ME, 9L));
    }
}
