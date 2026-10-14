package com.studybuddy.seed;

import com.studybuddy.auth.AccountCreation;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.InputRules;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DemoSeedServiceTest {
    @Test
    void seedUsesValidSyntheticContactsAndDoesNotResetExistingAccounts() {
        var creation = mock(AccountCreation.class);
        var users = mock(UserRepository.class);
        var students = mock(StudentRepository.class);
        var courses = mock(CourseRepository.class);
        var availability = mock(AvailabilitySlotRepository.class);
        var lock = mock(DatabaseMutationLock.class);
        var user = new User("jamie@demo.example.test", "hash", Role.STUDENT);
        ReflectionTestUtils.setField(user, "id", 2L);
        var student = new Student(user, "Jamie Lee", "SOB", "Business Management", 3, "+12025550102");
        when(users.existsByEmail(anyString())).thenReturn(true);
        when(users.existsByEmail("jamie@demo.example.test")).thenReturn(false, true);
        when(courses.findByCode(anyString())).thenReturn(Optional.of(new Course("IS442", "Object Oriented Programming")));
        when(creation.create("jamie@demo.example.test", "student-password", Role.STUDENT,
            "Jamie Lee", "SOB", "Business Management", 3, "+12025550102")).thenReturn(user);
        when(students.findById(2L)).thenReturn(Optional.of(student));
        var seed = new DemoSeedService(new DemoSeedProperties(true, "student-password", "admin-password"),
            creation, users, students, courses, availability, lock);

        seed.seed();
        assertEquals("+12025550102", InputRules.contactNumber(student.getContactNumber()));
        student.setName("Edited demo account");
        student.setContactNumber("Legacy private value");
        seed.seed();

        assertEquals("Edited demo account", student.getName());
        assertEquals("Legacy private value", student.getContactNumber());
        verify(students, times(1)).findById(2L);
        verify(creation, times(1)).create("jamie@demo.example.test", "student-password", Role.STUDENT,
            "Jamie Lee", "SOB", "Business Management", 3, "+12025550102");
        verifyNoMoreInteractions(creation, students);
    }
}
