package com.studybuddy.matching;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.availability.WeeklyAvailability;
import com.studybuddy.matching.config.MatchingConfigService;
import com.studybuddy.matching.config.MatchingProperties;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.matching.scoring.MatchingWeights;
import com.studybuddy.matching.scoring.WeightedMatchScorer;
import com.studybuddy.matching.strategy.MatchingStrategy;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Finds and ranks study buddies for a student: filters the other active
 * students, scores each with the admin's weights, labels the quality, and orders
 * them with the admin's active strategy.
 */
@Service
@Transactional(readOnly = true)
public class MatchService {

    private final AccountAccess access;
    private final StudentRepository students;
    private final AvailabilitySlotRepository availabilitySlots;
    private final CourseRepository courses;
    private final MatchingConfigService config;
    private final MatchingProperties properties;
    private final MatchSearchFilter filter;
    private final WeightedMatchScorer scorer;
    private final MatchResultAssembler assembler;

    public MatchService(AccountAccess access, StudentRepository students, AvailabilitySlotRepository availabilitySlots,
            CourseRepository courses, MatchingConfigService config, MatchingProperties properties,
            MatchSearchFilter filter, WeightedMatchScorer scorer, MatchResultAssembler assembler) {
        this.access = access;
        this.students = students;
        this.availabilitySlots = availabilitySlots;
        this.courses = courses;
        this.config = config;
        this.properties = properties;
        this.filter = filter;
        this.scorer = scorer;
        this.assembler = assembler;
    }

    public List<MatchResultDto> search(MatchSearchCriteria search, Long actorId) {
        Student self = access.requireStudent(actorId);
        validate(search);
        Map<Long, WeeklyAvailability> availability = availabilityByStudent();
        MatchingProfile searcher = profileOf(self, availability);
        MatchingWeights weights = config.currentWeights();
        MatchingStrategy strategy = config.activeStrategy();
        QualityThresholds thresholds = properties.quality();
        MatchQuality minimum = search.minQuality() == null ? MatchQuality.FAIR : search.minQuality();

        return students.findAllById(access.eligibleStudentIds()).stream()
                .filter(student -> !student.getId().equals(self.getId()))
                .map(student -> profileOf(student, availability))
                .filter(candidate -> filter.accepts(search, searcher, candidate))
                .map(candidate -> new ScoredCandidate(candidate, scorer.score(searcher, candidate, search, weights)))
                .filter(scored -> thresholds.classify(scored.score().total()).isAtLeast(minimum))
                .sorted(Comparator.comparing(ScoredCandidate::score, strategy.ranking())
                        .thenComparing(scored -> scored.profile().student().getName()))
                .map(scored -> assembler.toDto(searcher, scored.profile(), thresholds.classify(scored.score().total())))
                .toList();
    }

    private void validate(MatchSearchCriteria search) {
        if (search.courseId() == null && search.studyGoal() == null) {
            throw new InvalidMatchSearchException("Choose a course or a study goal to search");
        }
        if (search.minSharedHours() != null && search.minSharedHours() < 1) {
            throw new InvalidMatchSearchException("Minimum shared hours must be at least 1");
        }
        if (search.courseId() != null && !courses.existsById(search.courseId())) {
            throw new CourseNotFoundException(search.courseId());
        }
    }

    /** Loads every student's slots in one query instead of one query per candidate. */
    private Map<Long, WeeklyAvailability> availabilityByStudent() {
        return availabilitySlots.findAll().stream()
                .collect(Collectors.groupingBy(slot -> slot.getStudent().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), WeeklyAvailability::from)));
    }

    private static MatchingProfile profileOf(Student student, Map<Long, WeeklyAvailability> availability) {
        return new MatchingProfile(student, availability.getOrDefault(student.getId(), new WeeklyAvailability(List.<TimeSlot>of())));
    }

    private record ScoredCandidate(MatchingProfile profile, MatchScore score) {
    }
}
