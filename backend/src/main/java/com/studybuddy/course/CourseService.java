package com.studybuddy.course;

import com.studybuddy.common.AccountAccess;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courses;
    private final CourseAssembler assembler;
    private final AccountAccess access;

    public CourseService(CourseRepository courses, CourseAssembler assembler, AccountAccess access) {
        this.courses = courses;
        this.assembler = assembler;
        this.access = access;
    }

    public List<CourseDto> list(Long actorId) {
        access.requireStudent(actorId);
        return courses.findAll(Sort.by("code")).stream().map(assembler::toDto).toList();
    }
}
