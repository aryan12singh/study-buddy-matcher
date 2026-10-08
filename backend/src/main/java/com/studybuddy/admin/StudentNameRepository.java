package com.studybuddy.admin;

import com.studybuddy.student.Student;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Reads only student names for the admin account list, instead of loading full profiles. */
public interface StudentNameRepository extends Repository<Student, Long> {

    record StudentName(Long id, String name) {
    }

    @Query("select new com.studybuddy.admin.StudentNameRepository$StudentName(s.id, s.name) from Student s")
    List<StudentName> findAllNames();
}
