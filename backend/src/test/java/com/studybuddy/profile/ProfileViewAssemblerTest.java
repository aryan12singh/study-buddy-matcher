package com.studybuddy.profile;

import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionAssembler;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.connection.ConnectionService;
import com.studybuddy.course.Course;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

/**
 * Uses the real relationship assembler over mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class ProfileViewAssemblerTest {

    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    private static final Long CONNECTION_ID = 7L;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private NotificationService notificationService;

    @Mock private com.studybuddy.matchrequest.MatchRequestRepository matchRequestRepository;
    @Mock private com.studybuddy.student.AvailabilitySlotRepository availabilitySlotRepository;
    private ProfileViewAssembler assembler;
    private Student alice;
    private Student bob;

    @BeforeEach
    void setUp() {
        assembler = new ProfileViewAssembler(new ProfileRelationshipAssembler(connectionRepository, matchRequestRepository), availabilitySlotRepository);
        alice = student(ALICE_ID, "Alice");
        bob = student(BOB_ID, "Bob");
    }

    @Test
    void strangerGetsPublicProfile() {
        when(connectionRepository.findActiveBetween(ALICE_ID, CAROL_ID)).thenReturn(Optional.empty());

        ProfileDto profile = assembler.assemble(alice, CAROL_ID);

        assertInstanceOf(PublicProfileDto.class, profile);
    }

    @Test
    void activeConnectionGetsContactNumber() {
        when(connectionRepository.findActiveBetween(ALICE_ID, BOB_ID)).thenReturn(Optional.of(new Connection(alice,bob)));

        ProfileDto profile = assembler.assemble(alice, BOB_ID);

        ConnectedProfileDto connected = assertInstanceOf(ConnectedProfileDto.class, profile);
        assertEquals("+65 9000 0001", connected.contactNumber());
    }

    @Test
    void studentSeesTheirOwnContactNumber() {
        ProfileDto profile = assembler.assemble(alice, ALICE_ID);

        ConnectedProfileDto connected = assertInstanceOf(ConnectedProfileDto.class, profile);
        assertEquals("+65 9000 0001", connected.contactNumber());
    }

    @Test
    void endingTheConnectionHidesTheContactNumberAgain() {
        Connection connection = new Connection(alice, bob);
        ReflectionTestUtils.setField(connection, "id", CONNECTION_ID);
        // The query filters on endedAt; answer from the entity's own state.
        when(connectionRepository.findActiveBetween(ALICE_ID, BOB_ID)).thenAnswer(call -> connection.isActive()?Optional.of(connection):Optional.empty());
        assertInstanceOf(ConnectedProfileDto.class, assembler.assemble(alice, BOB_ID));

        connection.end();

        assertInstanceOf(PublicProfileDto.class, assembler.assemble(alice, BOB_ID));
    }

    @Test
    void publicProfileCarriesEveryNonPrivateField() {
        Course is442 = course(3L, "IS442");
        Course cs101 = course(4L, "CS101");
        alice.getCoursesTaken().addAll(Set.of(is442, cs101));
        alice.setTargetCourse(is442);
        alice.setPreferredStudyMode(StudyMode.ONLINE);
        alice.getStudyGoals().add(StudyGoal.EXAM_PREPARATION);
        alice.setPreferredGroupSizeMin(2);
        alice.setPreferredGroupSizeMax(3);
        when(connectionRepository.findActiveBetween(ALICE_ID, CAROL_ID)).thenReturn(Optional.empty());

        PublicProfileDto profile = (PublicProfileDto) assembler.assemble(alice, CAROL_ID);

        assertEquals("Alice", profile.name());
        assertEquals("SCIS", profile.school());
        assertEquals("Information Systems", profile.programme());
        assertEquals(3, profile.yearOfStudy());
        assertEquals(List.of("CS101", "IS442"), profile.coursesTaken().stream().map(ProfileCourseDto::code).toList());
        assertEquals("IS442", profile.targetCourse().code());
        assertEquals(StudyMode.ONLINE, profile.preferredStudyMode());
        assertEquals(Set.of(StudyGoal.EXAM_PREPARATION), profile.studyGoals());
        assertEquals(2, profile.preferredGroupSizeMin());
        assertEquals(3, profile.preferredGroupSizeMax());
    }

    @Test
    void publicProfileDtoHasNoContactField() {
        boolean hasContactComponent = Arrays.stream(PublicProfileDto.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("contact"));
        boolean hasContactField = Arrays.stream(PublicProfileDto.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(name -> name.toLowerCase().contains("contact"));

        assertFalse(hasContactComponent);
        assertFalse(hasContactField);
    }

    private static Student student(Long id, String name) {
        Student student = new Student(null, name, "SCIS", "Information Systems", 3, "+65 9000 000" + id);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static Course course(Long id, String code) {
        Course course = new Course(code, code + " course");
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
