package com.studybuddy.seed;

import com.studybuddy.seed.DemoData.PlanEntry;
import com.studybuddy.seed.DemoData.SessionPlanEntry;
import com.studybuddy.studyplan.PersonalPlanInput;
import com.studybuddy.studyplan.RoadmapDraft;
import com.studybuddy.studyplan.RoadmapFrame;
import com.studybuddy.studyplan.SessionMeetingFrames;
import com.studybuddy.studyplan.StudyMaterialReader;
import com.studybuddy.studyplan.StudyPlan;
import com.studybuddy.studyplan.StudyPlanCalendar;
import com.studybuddy.studyplan.StudyPlanCreationService;
import com.studybuddy.studyplan.StudyPlanDto;
import com.studybuddy.studyplan.StudyPlanEntryDto;
import com.studybuddy.studyplan.StudyPlanOutputValidator;
import com.studybuddy.studyplan.StudyPlanProgressInput;
import com.studybuddy.studyplan.StudyPlanRepository;
import com.studybuddy.studyplan.StudyPlanService;
import com.studybuddy.studysession.StudySession;
import com.studybuddy.studysession.StudySessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.studybuddy.seed.DemoSessionSeeder.sessionIdOf;
import static com.studybuddy.seed.DemoStudentSeeder.courseIdOf;

/**
 * Creates the demo's study plans from roadmaps written in advance, so seeding needs no Gemini key,
 * then ticks off the entries the data names. The roadmaps pass the same checks as Gemini's reply.
 * A personal plan's exam date counts from today in the study-plan time zone, as the plan's weeks
 * do, so the plan has the same number of weeks whatever hour the seed runs.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoPlanSeeder {

    private final StudyPlanCreationService creation;
    private final StudyPlanService plans;
    private final StudyPlanCalendar calendar;
    private final StudySessionRepository sessions;
    private final SessionMeetingFrames meetings;
    private final StudyMaterialReader materials;
    private final StudyPlanOutputValidator validator;
    private final StudyPlanRepository planRecords;
    private final JsonMapper json;
    private final Clock clock;

    public DemoPlanSeeder(StudyPlanCreationService creation, StudyPlanService plans, StudyPlanCalendar calendar,
            StudySessionRepository sessions, SessionMeetingFrames meetings, StudyMaterialReader materials,
            StudyPlanOutputValidator validator, StudyPlanRepository planRecords, JsonMapper json, Clock clock) {
        this.creation = creation;
        this.plans = plans;
        this.calendar = calendar;
        this.sessions = sessions;
        this.meetings = meetings;
        this.materials = materials;
        this.validator = validator;
        this.planRecords = planRecords;
        this.json = json;
        this.clock = clock;
    }

    public void seed(DemoData data, DemoRoster roster, Map<String, Long> courseIds, Map<String, Long> sessionIds) {
        for (PlanEntry entry : data.plans()) {
            seedPersonal(entry, roster, courseIds);
        }
        for (SessionPlanEntry entry : data.sessionPlans()) {
            seedSession(entry, roster, sessionIds);
        }
    }

    private void seedPersonal(PlanEntry entry, DemoRoster roster, Map<String, Long> courseIds) {
        Long student = roster.id(entry.student());
        LocalDate examDate = calendar.today().plusDays(entry.examInDays());
        PersonalPlanInput input = new PersonalPlanInput(courseIdOf(entry.course(), courseIds), entry.hoursPerWeek(),
                examDate, entry.goal(), entry.topics());
        StudyPlanDto plan = creation.createPersonalPrewritten(student, input, json.writeValueAsString(entry.roadmap()));
        for (String title : entry.completed()) {
            plans.markEntry(plan.id(), entryIdOf(plan, title), student, new StudyPlanProgressInput(true));
        }
    }

    /**
     * Builds the session's roadmap as a saved one is built: the session's upcoming meetings give
     * the frame, the roadmap is checked against it like Gemini's reply, and the plan covers the
     * group's course.
     */
    private void seedSession(SessionPlanEntry entry, DemoRoster roster, Map<String, Long> sessionIds) {
        StudySession session = sessions.findById(sessionIdOf(entry.session(), sessionIds)).orElseThrow();
        if (session.getGroup() == null) {
            throw new IllegalStateException("Demo session roadmaps are for group sessions, which have a course");
        }
        RoadmapFrame frame = meetings.upcoming(session);
        RoadmapDraft draft = validator.validate(json.writeValueAsString(fitted(entry.roadmap(), frame)), frame)
                .orElseThrow(() -> new IllegalStateException("Demo roadmap does not fit " + entry.session() + "'s meetings"));
        planRecords.save(StudyPlan.forSession(session, session.getGroup().getCourse(), entry.goal(), frame,
                materials.read(List.of(), entry.topics()), draft, roster.id(entry.organiser()), clock.instant()));
    }

    /** The roadmap without entries for meetings beyond those the session still has. */
    private JsonNode fitted(JsonNode roadmap, RoadmapFrame frame) {
        ObjectNode fitted = json.createObjectNode();
        fitted.set("sourceTopics", roadmap.path("sourceTopics"));
        ArrayNode entries = fitted.putArray("entries");
        for (JsonNode entry : roadmap.path("entries")) {
            if (frame.block(entry.path("sequence").asInt()).isPresent()) {
                entries.add(entry);
            }
        }
        return fitted;
    }

    private static Long entryIdOf(StudyPlanDto plan, String title) {
        return plan.entries().stream().filter(entry -> entry.title().equals(title))
                .map(StudyPlanEntryDto::id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo plan has no entry " + title));
    }
}
