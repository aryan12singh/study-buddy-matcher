package com.studybuddy.seed;

import com.studybuddy.auth.AccountCreation;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.InputRules;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.seed.DemoData.CourseEntry;
import com.studybuddy.user.Role;
import com.studybuddy.user.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Populates an opt-in demo dataset in one transaction: courses, an administrator, 50 students
 * with varied preferences and, on a fresh database, the history the live demo starts from.
 * Anything that already exists is left untouched, so running it again changes nothing.
 */
@Service
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoSeedService {

    private static final String ADMIN_EMAIL = "admin" + DemoStudentSeeder.EMAIL_DOMAIN;

    private final DemoSeedProperties properties;
    private final DemoDataLoader loader;
    private final DemoStudentSeeder students;
    private final DemoScenarioSeeder scenario;
    private final AccountCreation creation;
    private final UserRepository users;
    private final CourseRepository courses;
    private final DatabaseMutationLock mutationLock;

    public DemoSeedService(DemoSeedProperties properties, DemoDataLoader loader, DemoStudentSeeder students,
            DemoScenarioSeeder scenario, AccountCreation creation, UserRepository users, CourseRepository courses,
            DatabaseMutationLock mutationLock) {
        this.properties = properties;
        this.loader = loader;
        this.students = students;
        this.scenario = scenario;
        this.creation = creation;
        this.users = users;
        this.courses = courses;
        this.mutationLock = mutationLock;
    }

    @Transactional
    public void seed() {
        InputRules.password(properties.studentPassword());
        InputRules.password(properties.adminPassword());
        mutationLock.exclusive();
        DemoData data = loader.load();
        Map<String, Long> courseIds = seedCourses(data);
        Long adminId = seedAdmin();
        DemoRoster roster = students.seed(data, courseIds, properties.studentPassword());
        // History is replayed only onto a fresh roster; replaying it over existing accounts could clash.
        if (roster.isFresh()) {
            scenario.seed(data, roster, courseIds, adminId);
        }
    }

    private Map<String, Long> seedCourses(DemoData data) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (CourseEntry entry : data.courses()) {
            Course course = courses.findByCode(entry.code())
                    .orElseGet(() -> courses.save(new Course(entry.code(), entry.name())));
            ids.put(entry.code(), course.getId());
        }
        return ids;
    }

    private Long seedAdmin() {
        return users.findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> creation.create(ADMIN_EMAIL, properties.adminPassword(), Role.ADMIN,
                        null, null, null, null, null))
                .getId();
    }
}
