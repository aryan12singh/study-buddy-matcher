package com.studybuddy.profile;

import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileViewServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProfileViewAssembler profileViewAssembler;

    private ProfileViewService service;

    @BeforeEach
    void setUp() {
        service = new ProfileViewService(studentRepository, profileViewAssembler);
    }

    @Test
    void viewReturnsWhateverTheAssemblerDecides() {
        Student alice = new Student(null, "Alice", "SCIS", "Information Systems", 3, "+65 9000 0001");
        ReflectionTestUtils.setField(alice, "id", 1L);
        ProfileDto assembled = new PublicProfileDto(1L, "Alice", "SCIS", "Information Systems", 3,
                List.of(), null, null, Set.of(), null, null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(profileViewAssembler.assemble(alice, 2L)).thenReturn(assembled);

        assertSame(assembled, service.view(1L, 2L));
    }

    @Test
    void viewingUnknownStudentThrowsNotFound() {
        when(studentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> service.view(9L, 2L));

        verify(profileViewAssembler, never()).assemble(any(), anyLong());
    }
}
