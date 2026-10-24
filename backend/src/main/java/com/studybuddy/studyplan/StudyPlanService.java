package com.studybuddy.studyplan;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Reads, ticks off and deletes study plans, and does the two short database steps of creating one.
 * The AI call between those steps is made by {@link StudyPlanCreationService}, outside any
 * transaction, so no connection or lock is held while Gemini answers.
 */
@Service
@Transactional
public class StudyPlanService {
    private final StudyPlanRepository plans;
    private final CourseRepository courses;
    private final AccountAccess accounts;
    private final StudyPlanAccess access;
    private final StudyPlanCalendar calendar;
    private final StudyPlanAssembler assembler;
    private final Clock clock;

    public StudyPlanService(StudyPlanRepository plans, CourseRepository courses, AccountAccess accounts,
            StudyPlanAccess access, StudyPlanCalendar calendar, StudyPlanAssembler assembler, Clock clock) {
        this.plans = plans;
        this.courses = courses;
        this.accounts = accounts;
        this.access = access;
        this.calendar = calendar;
        this.assembler = assembler;
        this.clock = clock;
    }

    /** Checks everything that needs no AI, so a bad request never spends the student's allowance. */
    @Transactional(readOnly = true)
    public PersonalPlanRequest preparePersonal(Long actorId, PersonalPlanInput input) {
        accounts.requireStudent(actorId);
        Course course = courses.findById(input.courseId())
                .orElseThrow(() -> new InvalidInputException("courseId", "Choose one of the listed courses"));
        RoadmapFrame frame = calendar.personalWeeks(input.examDate(), input.hoursPerWeek());
        return new PersonalPlanRequest(course, input.studyGoal(), input.examDate(), frame);
    }

    /** Saves a validated roadmap, after checking again that the student is still active. */
    public StudyPlanDto savePersonal(Long actorId, PersonalPlanRequest request, StudyMaterial material, RoadmapDraft draft) {
        accounts.beginWrite();
        Student student = accounts.requireStudent(actorId);
        Long courseId = request.course().getId();
        Course course = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        StudyPlan plan = plans.save(StudyPlan.personal(student, course, request.goal(), request.examDate(),
                request.frame(), material, draft, clock.instant()));
        return assembler.toDto(plan, true);
    }

    /** The student's own plans, newest first. */
    @Transactional(readOnly = true)
    public List<StudyPlanSummaryDto> listMine(Long actorId) {
        accounts.requireStudent(actorId);
        return plans.findByStudentIdOrderByCreatedAtDescIdDesc(actorId).stream().map(assembler::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanDto get(Long planId, Long actorId) {
        accounts.requireStudent(actorId);
        StudyPlan plan = found(planId);
        access.requireReadable(plan, actorId);
        return assembler.toDto(plan, access.canManage(plan, actorId));
    }

    public StudyPlanDto markEntry(Long planId, Long entryId, Long actorId, StudyPlanProgressInput input) {
        accounts.beginWrite();
        accounts.requireStudent(actorId);
        StudyPlan plan = found(planId);
        access.requireManageable(plan, actorId);
        if (!plan.markEntry(entryId, input.completed(), clock.instant())) {
            throw new StudyPlanEntryNotFoundException(planId, entryId);
        }
        return assembler.toDto(plan, true);
    }

    public void delete(Long planId, Long actorId) {
        accounts.beginWrite();
        accounts.requireStudent(actorId);
        StudyPlan plan = found(planId);
        access.requireManageable(plan, actorId);
        plans.delete(plan);
    }

    private StudyPlan found(Long planId) {
        return plans.findById(planId).orElseThrow(() -> new StudyPlanNotFoundException(planId));
    }
}
