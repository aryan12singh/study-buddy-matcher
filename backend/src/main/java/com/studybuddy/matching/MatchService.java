package com.studybuddy.matching;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.availability.WeeklyAvailability;
import com.studybuddy.matching.config.MatchingConfigService;
import com.studybuddy.matching.config.MatchingProperties;
import com.studybuddy.matching.config.MatchingConfigSnapshot;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.matching.scoring.WeightedMatchScorer;
import com.studybuddy.matching.strategy.MatchingStrategy;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds and ranks study buddies for a student: filters the other active
 * students, scores each with the admin's weights, labels the quality, and orders
 * them with the admin's active strategy. One database snapshot keeps the strategy
 * and its weights together even when an administrator saves during a search.
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
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
    private final MatchRelationships relationships;

    public MatchService(AccountAccess access, StudentRepository students, AvailabilitySlotRepository availabilitySlots,
            CourseRepository courses, MatchingConfigService config, MatchingProperties properties,
            MatchSearchFilter filter, WeightedMatchScorer scorer, MatchResultAssembler assembler,
            MatchRelationships relationships) {
        this.access = access;
        this.students = students;
        this.availabilitySlots = availabilitySlots;
        this.courses = courses;
        this.config = config;
        this.properties = properties;
        this.filter = filter;
        this.scorer = scorer;
        this.assembler = assembler;
        this.relationships = relationships;
    }

    public MatchSearchResultDto search(MatchSearchCriteria search, Long actorId) {
        Student self = access.requireStudent(actorId);
        validate(search);
        List<Long> eligible = access.eligibleStudentIds();
        List<Student> pool = students.findWithMatchingDetailsByIdIn(eligible);
        Map<Long, WeeklyAvailability> availability = availabilityOf(eligible, self.getId());
        MatchingProfile searcher = profileOf(self, availability);
        MatchingConfigSnapshot settings = config.snapshot();
        MatchingStrategy strategy = settings.strategy();
        MatchQuality minimum = search.minQuality() == null ? MatchQuality.FAIR : search.minQuality();
        Map<Long, RelationshipState> standing = relationships.of(self.getId());

        MatchSort sort = search.sort() == null ? MatchSort.BEST_MATCH : search.sort();
        int limit = search.limit() == null ? properties.defaultResultLimit() : search.limit();
        List<ScoredCandidate> ranked = pool.stream()
                .filter(student -> !student.getId().equals(self.getId()))
                .filter(student -> standing.get(student.getId()) != RelationshipState.CONNECTED)
                .map(student -> profileOf(student, availability))
                .filter(candidate -> filter.accepts(search, searcher, candidate))
                .map(candidate -> new ScoredCandidate(candidate, scorer.score(searcher, candidate, search, settings.weights(),
                        settings.enabledCriteria())))
                .filter(scored -> scored.score().total() >= settings.minScore())
                .filter(scored -> scored.score().quality().isAtLeast(minimum))
                .sorted(ordering(sort, searcher, settings))
                .toList();
        List<MatchResultDto> matches = ranked.stream().limit(limit)
                .map(scored -> assembler.toDto(searcher, scored.profile(), scored.score(), search, settings.enabledCriteria(),
                        standing.getOrDefault(scored.profile().student().getId(), RelationshipState.STRANGER)))
                .toList();
        return new MatchSearchResultDto(strategy.type(), settings.version(), sort, limit, ranked.size(), matches);
    }

    /**
     * The student's sort first, then the strategy, then more shared hours, then name and id
     * so that equal matches always appear in the same order.
     */
    private static Comparator<ScoredCandidate> ordering(MatchSort sort, MatchingProfile searcher,
            MatchingConfigSnapshot settings) {
        return sort.before(searcher)
                .thenComparing(ScoredCandidate::score, settings.strategy().ranking(settings.enabledCriteria()))
                .thenComparing(Comparator.comparingDouble((ScoredCandidate candidate) -> candidate.score().sharedHoursPerWeek()).reversed())
                .thenComparing(candidate -> candidate.profile().student().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.profile().student().getId());
    }

    private void validate(MatchSearchCriteria search) {
        if (search == null || (search.courseId() == null && search.studyGoal() == null)) {
            throw new InvalidMatchSearchException("Choose a course or study goal before searching");
        }
        if (search.limit() != null && (search.limit() < 1 || search.limit() > properties.maxResultLimit())) {
            throw new InvalidMatchSearchException("Result limit must be between 1 and " + properties.maxResultLimit());
        }
        if (search.minSharedHours() != null && search.minSharedHours() < 1) {
            throw new InvalidMatchSearchException("Minimum shared hours must be at least 1");
        }
        if (search.courseId() != null && search.courseId() <= 0) {
            throw new InvalidMatchSearchException("Course id must be positive");
        }
        if (search.courseId() != null && !courses.existsById(search.courseId())) {
            throw new CourseNotFoundException(search.courseId());
        }
    }

    /** Loads the searcher's and every eligible student's slots in one query; inactive students' are never read. */
    private Map<Long, WeeklyAvailability> availabilityOf(List<Long> eligible, Long selfId) {
        Set<Long> ids = new HashSet<>(eligible);
        ids.add(selfId);
        return availabilitySlots.findByStudentIdIn(ids).stream()
                .collect(Collectors.groupingBy(slot -> slot.getStudent().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), WeeklyAvailability::from)));
    }

    private static MatchingProfile profileOf(Student student, Map<Long, WeeklyAvailability> availability) {
        return new MatchingProfile(student, availability.getOrDefault(student.getId(), new WeeklyAvailability(List.<TimeSlot>of())));
    }
}
