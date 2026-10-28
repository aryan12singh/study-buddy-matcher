package com.studybuddy.seed;

import com.studybuddy.seed.DemoData.PlanEntry;
import com.studybuddy.studyplan.PersonalPlanInput;
import com.studybuddy.studyplan.StudyPlanCreationService;
import com.studybuddy.studyplan.StudyPlanDto;
import com.studybuddy.studyplan.StudyPlanEntryDto;
import com.studybuddy.studyplan.StudyPlanProgressInput;
import com.studybuddy.studyplan.StudyPlanService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import static com.studybuddy.seed.DemoStudentSeeder.courseIdOf;

/**
 * Creates the demo's study plans from roadmaps written in advance, so seeding needs no Gemini key,
 * then ticks off the entries the data names. The roadmaps pass the same checks as Gemini's reply.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoPlanSeeder {

    private final StudyPlanCreationService creation;
    private final StudyPlanService plans;
    private final JsonMapper json;
    private final Clock clock;

    public DemoPlanSeeder(StudyPlanCreationService creation, StudyPlanService plans, JsonMapper json, Clock clock) {
        this.creation = creation;
        this.plans = plans;
        this.json = json;
        this.clock = clock;
    }

    public void seed(DemoData data, DemoRoster roster, Map<String, Long> courseIds) {
        for (PlanEntry entry : data.plans()) {
            Long student = roster.id(entry.student());
            LocalDate examDate = LocalDate.now(clock).plusDays(entry.examInDays());
            PersonalPlanInput input = new PersonalPlanInput(courseIdOf(entry.course(), courseIds), entry.hoursPerWeek(),
                    examDate, entry.goal(), entry.topics());
            StudyPlanDto plan = creation.createPersonalPrewritten(student, input, json.writeValueAsString(entry.roadmap()));
            for (String title : entry.completed()) {
                plans.markEntry(plan.id(), entryIdOf(plan, title), student, new StudyPlanProgressInput(true));
            }
        }
    }

    private static Long entryIdOf(StudyPlanDto plan, String title) {
        return plan.entries().stream().filter(entry -> entry.title().equals(title))
                .map(StudyPlanEntryDto::id).findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo plan has no entry " + title));
    }
}
