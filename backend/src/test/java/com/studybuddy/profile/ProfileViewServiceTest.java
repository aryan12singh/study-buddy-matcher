package com.studybuddy.profile;

import com.studybuddy.student.StudentNotFoundException;
import com.studybuddy.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
    private com.studybuddy.common.AccountAccess access;

    @Mock
    private ProfileViewAssembler profileViewAssembler;

    private ProfileViewService service;

    @BeforeEach
    void setUp() {
        service = new ProfileViewService(access, profileViewAssembler);
    }

    @Test
    void viewReturnsWhateverTheAssemblerDecides() {
        Student alice = new Student(null, "Alice", "SCIS", "Information Systems", 3, "+65 9000 0001");
        ReflectionTestUtils.setField(alice, "id", 1L);
        ProfileDto assembled = new PublicProfileDto(1L, "Alice", "SCIS", "Information Systems", 3,
                List.of(), null, null, Set.of(), null, null, List.of(), new ProfileRelationshipDto(RelationshipState.STRANGER,null,null));
        when(access.eligibleStudent(1L)).thenReturn(alice);
        when(profileViewAssembler.assemble(alice, 2L)).thenReturn(assembled);

        assertSame(assembled, service.view(1L, 2L));
    }

    @Test
    void viewingUnknownStudentThrowsNotFound() {
        when(access.eligibleStudent(9L)).thenThrow(new StudentNotFoundException(9L));

        assertThrows(StudentNotFoundException.class, () -> service.view(9L, 2L));

        verify(profileViewAssembler, never()).assemble(any(), anyLong());
    }
}
