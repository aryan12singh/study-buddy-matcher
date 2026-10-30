package com.studybuddy.student;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Stores student profiles and serves the matching engine: the ids of active students, and those
 * students loaded with their courses and goals in one query.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("select s.id from Student s where s.user.active = true and s.user.role = com.studybuddy.user.Role.STUDENT order by s.id")
    List<Long> findActiveIds();

    /**
     * Loads students together with everything the matching engine reads, in one query,
     * instead of one extra query per student for each collection.
     */
    @EntityGraph(attributePaths = {"coursesTaken", "studyGoals", "targetCourse"})
    List<Student> findWithMatchingDetailsByIdIn(Collection<Long> ids);
}
