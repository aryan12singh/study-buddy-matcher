package com.studybuddy.profile;

import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Viewing another student's profile. The viewer's id is passed in explicitly
 * until authentication supplies it; what they may see is decided by
 * {@link ProfileViewAssembler}.
 */
@Service
@Transactional(readOnly = true)
public class ProfileViewService {

    private final StudentRepository studentRepository;
    private final ProfileViewAssembler profileViewAssembler;

    public ProfileViewService(StudentRepository studentRepository, ProfileViewAssembler profileViewAssembler) {
        this.studentRepository = studentRepository;
        this.profileViewAssembler = profileViewAssembler;
    }

    /**
     * @throws StudentNotFoundException if the student does not exist
     */
    public ProfileDto view(Long studentId, Long viewerId) {
        Student subject = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        return profileViewAssembler.assemble(subject, viewerId);
    }
}
