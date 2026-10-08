package com.studybuddy.student;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and replaces the signed-in student's own profile, preferences and weekly availability. */
@Service
@Transactional
public class ProfileService {
    private final AccountAccess access;
    private final CourseRepository courses;
    private final AvailabilitySlotRepository availability;
    private final AvailabilityRules availabilityRules;
    private final MyProfileAssembler assembler;
    private final ProfileProperties properties;
    private final DatabaseMutationLock mutationLock;

    public ProfileService(AccountAccess access, CourseRepository courses, AvailabilitySlotRepository availability,
        AvailabilityRules availabilityRules, MyProfileAssembler assembler, ProfileProperties properties,
        DatabaseMutationLock mutationLock) {
        this.access = access;
        this.courses = courses;
        this.availability = availability;
        this.availabilityRules = availabilityRules;
        this.assembler = assembler;
        this.properties = properties;
        this.mutationLock = mutationLock;
    }

    @Transactional(readOnly = true)
    public MyProfileDto getMine(Long actorId) {
        Student student = access.requireStudent(actorId);
        return assembler.toDto(student, availability.findByStudentId(actorId));
    }

    public MyProfileDto updateMine(Long actorId, UpdateProfileRequest request) {
        mutationLock.shared();
        Student student = access.requireStudent(actorId);
        if (request == null) {
            throw new InvalidInputException("Profile details are required");
        }
        String name = InputRules.required(request.name(), "Name", InputRules.TEXT_LIMIT);
        String school = InputRules.required(request.school(), "School", InputRules.TEXT_LIMIT);
        String programme = InputRules.required(request.programme(), "Programme", InputRules.TEXT_LIMIT);
        String contactNumber = InputRules.required(request.contactNumber(), "Contact number", InputRules.TEXT_LIMIT);
        InputRules.year(request.yearOfStudy());
        List<Course> taken = coursesTaken(request.courseIds());
        Course target = targetCourse(request.targetCourseId());
        StudyMode mode = required(request.preferredStudyMode(), "preferredStudyMode", "Choose a preferred study mode");
        GroupSizePreference groupSize = required(request.groupSizePreference(), "groupSizePreference",
            "Choose a preferred group size");

        student.setName(name);
        student.setSchool(school);
        student.setProgramme(programme);
        student.setYearOfStudy(request.yearOfStudy());
        student.setContactNumber(contactNumber);
        student.getCoursesTaken().clear();
        student.getCoursesTaken().addAll(taken);
        student.setTargetCourse(target);
        student.setPreferredStudyMode(mode);
        student.setPreferredGroupSizeMin(groupSize.minimumSize());
        student.setPreferredGroupSizeMax(groupSize.maximumSize(properties.groupSizeMax()));
        student.getStudyGoals().clear();
        student.getStudyGoals().addAll(studyGoals(request.studyGoals()));
        return assembler.toDto(student, availability.findByStudentId(actorId));
    }

    public List<AvailabilitySlotDto> replaceAvailability(Long actorId, ReplaceAvailabilityRequest request) {
        mutationLock.shared();
        Student student = access.requireStudent(actorId);
        List<AvailabilitySlotDto> slots = request == null ? null : request.slots();
        availabilityRules.validate(slots);
        availability.deleteByStudentId(actorId);
        availability.flush();
        List<AvailabilitySlot> saved = availability.saveAll(slots.stream()
            .map(slot -> new AvailabilitySlot(student, slot.dayOfWeek(), slot.startTime(), slot.endTime()))
            .toList());
        return assembler.toSlotDtos(saved);
    }

    private List<Course> coursesTaken(List<Long> courseIds) {
        List<Long> ids = courseIds == null ? List.of() : courseIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new InvalidInputException("courseIds", "Choose at least one course you are currently taking");
        }
        if (ids.size() > properties.maxCourses()) {
            throw new InvalidInputException("courseIds", "Choose at most " + properties.maxCourses() + " courses");
        }
        List<Course> found = courses.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new InvalidInputException("courseIds", "One or more courses do not exist");
        }
        return found;
    }

    private Course targetCourse(Long targetCourseId) {
        if (targetCourseId == null) {
            return null;
        }
        return courses.findById(targetCourseId).orElseThrow(() ->
            new InvalidInputException("targetCourseId", "The selected course does not exist"));
    }

    private static Set<StudyGoal> studyGoals(Set<StudyGoal> goals) {
        return goals == null ? Set.of() : goals.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static <T> T required(T value, String field, String message) {
        if (value == null) {
            throw new InvalidInputException(field, message);
        }
        return value;
    }
}
