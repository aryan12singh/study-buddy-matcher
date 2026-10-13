package com.studybuddy.student;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseAssembler;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    private static final long STUDENT_ID = 4L;
    private final ProfileProperties properties = new ProfileProperties(5, 8);
    private final Course oop = course(1L, "IS442");
    private final Course spm = course(2L, "IS212");
    private final Course bpas = course(3L, "IS210");

    @Mock private AccountAccess access;
    @Mock private CourseRepository courses;
    @Mock private AvailabilitySlotRepository availability;
    @Mock private DatabaseMutationLock mutationLock;
    private ProfileService service;
    private Student student;

    @BeforeEach void setUp() {
        service = new ProfileService(access, courses, availability, new AvailabilityRules(),
            new MyProfileAssembler(new CourseAssembler(), properties), properties, mutationLock);
        User user = new User("student04@demo.example.test", "hash", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", STUDENT_ID);
        student = new Student(user, "Demo Student 4", "SCIS", "Information Systems", 1, "Synthetic contact 4");
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
    }

    private static Course course(Long id, String code) {
        Course course = new Course(code, code + " name");
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    private static UpdateProfileRequest request(List<Long> courseIds, Long targetId, StudyMode mode, GroupSizePreference size) {
        return new UpdateProfileRequest(" Demo Student 4 ", "SCIS", "Information Systems", 2, " Synthetic contact 4 ",
            courseIds, targetId, mode, size, Set.of(StudyGoal.EXAM_PREPARATION));
    }

    @Test void readingTheOwnProfileIncludesEmailAndContactNumber() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(availability.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        MyProfileDto profile = service.getMine(STUDENT_ID);
        assertEquals("student04@demo.example.test", profile.email());
        assertEquals("Synthetic contact 4", profile.contactNumber());
        assertEquals(GroupSizePreference.EITHER, profile.groupSizePreference());
        assertEquals(5, profile.groupSizeMax());
    }

    @Test void updateReplacesEveryFieldAndStoresGroupSizeAsARange() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(courses.findAllById(List.of(2L, 1L))).thenReturn(List.of(spm, oop));
        when(courses.findById(3L)).thenReturn(Optional.of(bpas));
        when(availability.findByStudentId(STUDENT_ID)).thenReturn(List.of());

        MyProfileDto profile = service.updateMine(STUDENT_ID,
            request(List.of(2L, 1L, 2L), 3L, StudyMode.ONLINE, GroupSizePreference.SMALL_GROUP));

        verify(mutationLock).shared();
        assertEquals("Demo Student 4", student.getName());
        assertEquals("Synthetic contact 4", student.getContactNumber());
        assertEquals(2, student.getYearOfStudy());
        assertEquals(Set.of(oop, spm), student.getCoursesTaken());
        assertSame(bpas, student.getTargetCourse());
        assertEquals(StudyMode.ONLINE, student.getPreferredStudyMode());
        assertEquals(3, student.getPreferredGroupSizeMin());
        assertEquals(5, student.getPreferredGroupSizeMax());
        assertEquals(Set.of(StudyGoal.EXAM_PREPARATION), student.getStudyGoals());
        assertEquals(List.of("IS212", "IS442"), profile.coursesTaken().stream().map(c -> c.code()).toList());
        assertEquals(GroupSizePreference.SMALL_GROUP, profile.groupSizePreference());
    }

    @Test void aTargetCourseOutsideTheCoursesTakenIsAllowed() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(courses.findAllById(List.of(1L))).thenReturn(List.of(oop));
        when(courses.findById(3L)).thenReturn(Optional.of(bpas));
        when(availability.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.updateMine(STUDENT_ID,
            request(List.of(1L), 3L, StudyMode.EITHER, GroupSizePreference.EITHER)));
    }

    @Test void atLeastOneCourseIsRequired() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        var error = assertThrows(InvalidInputException.class, () -> service.updateMine(STUDENT_ID,
            request(List.of(), null, StudyMode.ONLINE, GroupSizePreference.EITHER)));
        assertTrue(error.getFieldErrors().containsKey("courseIds"));
        assertEquals("Demo Student 4", student.getName());
    }

    @Test void tooManyCoursesAreRejected() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        var error = assertThrows(InvalidInputException.class, () -> service.updateMine(STUDENT_ID,
            request(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), null, StudyMode.ONLINE, GroupSizePreference.EITHER)));
        assertTrue(error.getFieldErrors().containsKey("courseIds"));
        verifyNoInteractions(courses);
    }

    @Test void anUnknownCourseIsReportedOnTheCoursesField() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(courses.findAllById(List.of(1L, 99L))).thenReturn(List.of(oop));
        var error = assertThrows(InvalidInputException.class, () -> service.updateMine(STUDENT_ID,
            request(List.of(1L, 99L), null, StudyMode.ONLINE, GroupSizePreference.EITHER)));
        assertTrue(error.getFieldErrors().containsKey("courseIds"));
    }

    @Test void anUnknownTargetCourseIsReportedOnItsField() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(courses.findAllById(List.of(1L))).thenReturn(List.of(oop));
        when(courses.findById(99L)).thenReturn(Optional.empty());
        var error = assertThrows(InvalidInputException.class, () -> service.updateMine(STUDENT_ID,
            request(List.of(1L), 99L, StudyMode.ONLINE, GroupSizePreference.EITHER)));
        assertTrue(error.getFieldErrors().containsKey("targetCourseId"));
    }

    @Test void studyModeAndGroupSizeAreRequired() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(courses.findAllById(List.of(1L))).thenReturn(List.of(oop));
        var mode = assertThrows(InvalidInputException.class, () -> service.updateMine(STUDENT_ID,
            request(List.of(1L), null, null, GroupSizePreference.EITHER)));
        assertTrue(mode.getFieldErrors().containsKey("preferredStudyMode"));
        var size = assertThrows(InvalidInputException.class, () -> service.updateMine(STUDENT_ID,
            request(List.of(1L), null, StudyMode.ONLINE, null)));
        assertTrue(size.getFieldErrors().containsKey("groupSizePreference"));
    }

    @Test void anAdministratorCannotUseTheStudentProfile() {
        when(access.requireStudent(99L)).thenThrow(new ForbiddenActionException("A student account is required"));
        assertThrows(ForbiddenActionException.class, () -> service.updateMine(99L,
            request(List.of(1L), null, StudyMode.ONLINE, GroupSizePreference.EITHER)));
        assertThrows(ForbiddenActionException.class, () -> service.getMine(99L));
        verifyNoInteractions(courses, availability);
    }

    @Test void availabilityIsReplacedAndReturnedMondayFirst() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        when(availability.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        var slots = List.of(
            new AvailabilitySlotDto(DayOfWeek.THURSDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)),
            new AvailabilitySlotDto(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));

        var saved = service.replaceAvailability(STUDENT_ID, new ReplaceAvailabilityRequest(slots));

        var order = inOrder(availability);
        order.verify(availability).deleteByStudentId(STUDENT_ID);
        order.verify(availability).saveAll(anyList());
        assertEquals(List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), saved.stream().map(AvailabilitySlotDto::dayOfWeek).toList());
    }

    @Test void invalidAvailabilityChangesNothing() {
        when(access.requireStudent(STUDENT_ID)).thenReturn(student);
        var overlapping = List.of(
            new AvailabilitySlotDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
            new AvailabilitySlotDto(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));
        assertThrows(InvalidInputException.class,
            () -> service.replaceAvailability(STUDENT_ID, new ReplaceAvailabilityRequest(overlapping)));
        verify(availability, never()).deleteByStudentId(any());
        verify(availability, never()).saveAll(anyList());
    }
}
