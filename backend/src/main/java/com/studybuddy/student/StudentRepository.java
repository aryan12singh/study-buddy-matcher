package com.studybuddy.student;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("select s.id from Student s where s.user.active = true and s.user.role = com.studybuddy.user.Role.STUDENT order by s.id")
    List<Long> findActiveIds();
}
