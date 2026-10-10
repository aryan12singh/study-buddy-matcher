package com.studybuddy.matching;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.matching.config.MatchingConfigService;
import com.studybuddy.matching.config.MatchingProperties;
import com.studybuddy.matching.scoring.AvailabilityOverlapScorer;
import com.studybuddy.matching.scoring.CourseScorer;
import com.studybuddy.matching.scoring.GroupSizeScorer;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.scoring.StudyGoalScorer;
import com.studybuddy.matching.scoring.StudyModeScorer;
import com.studybuddy.matching.scoring.WeightedMatchScorer;
import com.studybuddy.matching.strategy.BalancedStrategy;
import com.studybuddy.student.AvailabilitySlot;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.ProfileProperties;
import com.studybuddy.student.Student;
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

import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.student;
import static com.studybuddy.matching.MatchingFixtures.evenWeights;
import static java.time.DayOfWeek.MONDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private MatchService service;
    private Student me;
    private Student strong;
    private Student weak;

    @BeforeEach
    void setUp() {
        MatchingProperties properties = MatchingFixtures.properties();
        WeightedMatchScorer scorer = new WeightedMatchScorer(List.of(new CourseScorer(), new AvailabilityOverlapScorer(),
                new StudyModeScorer(), new StudyGoalScorer(), new GroupSizeScorer()));
        service = new MatchService(access, students, availabilitySlots, courses, config, properties,
                new MatchSearchFilter(new ProfileProperties(5, 8)), scorer, new MatchResultAssembler());

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
        when(students.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(weak, me, strong));
        when(courses.existsById(4L)).thenReturn(true);
        when(availabilitySlots.findAll()).thenReturn(List.of(
                new AvailabilitySlot(me, MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new AvailabilitySlot(strong, MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new AvailabilitySlot(weak, MONDAY, LocalTime.of(10, 30), LocalTime.of(12, 0))));
        when(config.currentWeights()).thenReturn(new MatchingWeights(evenWeights()));
        when(config.activeStrategy()).thenReturn(new BalancedStrategy());
    }

    private static MatchSearchCriteria courseSearch(MatchQuality minQuality) {
        return new MatchSearchCriteria(4L, null, null, null, null, minQuality);
    }

    @Test
    void ranksOtherStudentsBestFirstAndNeverIncludesTheSearcher() {
        List<MatchResultDto> results = service.search(courseSearch(null), ME);

        assertEquals(List.of(2L, 3L), results.stream().map(MatchResultDto::studentId).toList());
        assertEquals(MatchQuality.STRONG, results.get(0).quality());
        assertEquals(2.0, results.get(0).sharedHoursPerWeek());
        assertEquals(0.5, results.get(1).sharedHoursPerWeek());
    }

    @Test
    void minimumQualityHidesWeakerMatches() {
        List<MatchResultDto> results = service.search(courseSearch(MatchQuality.STRONG), ME);

        assertEquals(List.of(2L), results.stream().map(MatchResultDto::studentId).toList());
    }

    @Test
    void filtersNarrowTheResults() {
        MatchSearchCriteria online = new MatchSearchCriteria(4L, null, StudyMode.ONLINE, null, null, null);

        assertEquals(List.of(2L), service.search(online, ME).stream().map(MatchResultDto::studentId).toList());
    }

    @Test
    void returnsAnEmptyListWhenNobodyFits() {
        MatchSearchCriteria planning = new MatchSearchCriteria(null, StudyGoal.PROJECT_DISCUSSION, null, null, null, null);

        assertTrue(service.search(planning, ME).isEmpty());
    }

    @Test
    void rejectsASearchWithNoCourseOrGoal() {
        assertThrows(InvalidMatchSearchException.class,
                () -> service.search(new MatchSearchCriteria(null, null, StudyMode.ONLINE, null, null, null), ME));
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
    void onlyStudentsCanSearch() {
        when(access.requireStudent(ME)).thenThrow(new ForbiddenActionException("A student account is required"));

        assertThrows(ForbiddenActionException.class, () -> service.search(courseSearch(null), ME));
        verifyNoInteractions(students);
    }
}
