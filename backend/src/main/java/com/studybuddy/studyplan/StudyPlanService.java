package com.studybuddy.studyplan;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ConflictException;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.Student;
import com.studybuddy.studysession.StudySession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

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
    private final SessionMeetingFrames meetings;
    private final StudyPlanAssembler assembler;
    private final Clock clock;

    public StudyPlanService(StudyPlanRepository plans, CourseRepository courses, AccountAccess accounts,
            StudyPlanAccess access, StudyPlanCalendar calendar, SessionMeetingFrames meetings,
            StudyPlanAssembler assembler, Clock clock) {
        this.plans = plans;
        this.courses = courses;
        this.accounts = accounts;
        this.access = access;
        this.calendar = calendar;
        this.meetings = meetings;
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

    /**
     * Checks everything about a session plan that needs no AI: the actor may manage the session, it
     * has meetings left to plan, and it has a course (the group's, or the one a buddy chose).
     */
    @Transactional(readOnly = true)
    public SessionPlanRequest prepareSession(Long actorId, Long sessionId, SessionPlanInput input) {
        accounts.requireStudent(actorId);
        StudyPlanAccess.SessionRole role = access.sessionRole(sessionId, actorId);
        if (!role.manager()) throw new ForbiddenActionException("Only the group leader can plan this session");
        StudySession session = role.session();
        Course course = session.getGroup() != null ? session.getGroup().getCourse() : chosenCourse(input.courseId());
        return new SessionPlanRequest(sessionId, session.getVersion(), course, input.studyGoal(), meetings.upcoming(session));
    }

    /**
     * Saves a validated session roadmap in place of the session's old one, holding the same locks
     * as a change to the session. Refuses if the session was edited while Gemini answered, since
     * the roadmap's dates would no longer match its meetings; a meeting that merely ended in the
     * meantime is left out of the saved roadmap instead.
     */
    public StudyPlanDto saveSession(Long actorId, SessionPlanRequest request, StudyMaterial material, RoadmapDraft draft) {
        accounts.beginWrite();
        StudySession session = access.lockForManaging(request.sessionId(), actorId);
        if (session.getVersion() != request.sessionVersion()) {
            throw new ConflictException("This session was changed while the roadmap was written; try again");
        }
        SessionMeetingFrames.StillUpcoming roadmap = meetings.dropEnded(session, request.frame(), draft);
        Long courseId = request.course().getId();
        Course course = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        plans.deleteAll(plans.findBySessionIdOrderByFirstBlockDateAscIdAsc(session.getId()));
        plans.flush();
        StudyPlan plan = plans.save(StudyPlan.forSession(session, course, request.goal(), roadmap.frame(),
                material, roadmap.draft(), actorId, clock.instant()));
        return assembler.toDto(plan, true);
    }

    /**
     * The session's roadmap, for any current participant; managers may also change it. Empty when
     * the session has no roadmap yet, which is a normal state rather than an error.
     */
    @Transactional(readOnly = true)
    public Optional<StudyPlanDto> getForSession(Long sessionId, Long actorId) {
        accounts.requireStudent(actorId);
        StudyPlanAccess.SessionRole role = access.sessionRole(sessionId, actorId);
        return plans.findFirstBySessionIdOrderByCreatedAtDescIdDesc(sessionId)
                .map(plan -> assembler.toDto(plan, role.manager()));
    }

    /** A buddy session has no course of its own, so the buddy planning it chooses one. */
    private Course chosenCourse(Long courseId) {
        if (courseId == null) throw new InvalidInputException("courseId", "Choose the course this session is for");
        return courses.findById(courseId).orElseThrow(() -> new InvalidInputException("courseId", "Choose one of the listed courses"));
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
