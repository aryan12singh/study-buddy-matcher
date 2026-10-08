package com.studybuddy.seed;

import com.studybuddy.auth.AccountCreation;
import com.studybuddy.common.DatabaseMutationLock;
import com.studybuddy.common.InputRules;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.student.AvailabilitySlot;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import com.studybuddy.user.Role;
import com.studybuddy.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoSeedService {
    private static final String DOMAIN = "@demo.example.test";
    private static final String[][] COURSES = {
        {
            "IS442", "Object Oriented Programming"
        }, {
            "IS212", "Software Project Management"
        },
        {
            "IS210", "Business Process Analysis and Solutioning"
        }, {
            "IS214", "Enterprise Solution Development"
        },
        {
            "IS216", "Web Application Development II"
        }, {
            "IS111", "Introduction to Programming"
        },
        {
            "IS112", "Data Management"
        }, {
            "IS113", "Web Application Development I"
        },
        {
            "IS114", "Computing Fundamentals"
        }, {
            "IS115", "Algorithms and Data Structures"
        }
    };
    private final DemoSeedProperties properties;
    private final AccountCreation creation;
    private final UserRepository users;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final AvailabilitySlotRepository availability;
    private final DatabaseMutationLock mutationLock;

    public DemoSeedService(DemoSeedProperties properties, AccountCreation creation, UserRepository users, StudentRepository students,
        CourseRepository courses, AvailabilitySlotRepository availability, DatabaseMutationLock mutationLock) {
        this.properties = properties;
        this.creation = creation;
        this.users = users;
        this.students = students;
        this.courses = courses;
        this.availability = availability;
        this.mutationLock = mutationLock;
    }

    @Transactional
    public void seed() {
        if (!properties.enabled()) {
            return;
        }
        InputRules.password(properties.studentPassword());
        InputRules.password(properties.adminPassword());
        mutationLock.exclusive();
        var seededCourses = new ArrayList<Course>();
        for (var entry : COURSES) seededCourses.add(courses.findByCode(entry[0]).orElseGet(() -> courses.save(new Course(entry[0], entry[1]))));
        String adminEmail = "admin" + DOMAIN;
        if (!users.existsByEmail(adminEmail)) creation.create(adminEmail, properties.adminPassword(), Role.ADMIN, null, null, null, null, null);
        for (int number = 1; number <= 50; number++) {
            String handle = number == 1 ? "priya" : number == 2 ? "jamie" : number == 3 ? "alex" : "student" + String.format("%02d", number);
            String email = handle + DOMAIN;
            // Existing identities are left intact, even if their data differs.
            if (users.existsByEmail(email)) continue;
            String name = number == 1 ? "Priya Nair" : number == 2 ? "Jamie Lee" : number == 3 ? "Alex Tan" : "Demo Student " + number;
            String school = number%3 == 0 ? "SOE" : number%3 == 1 ? "SCIS" : "SOB";
            String programme = number%3 == 0 ? "Economics" : number%3 == 1 ? "Information Systems" : "Business Management";
            var user = creation.create(email, properties.studentPassword(), Role.STUDENT, name, school, programme, 1 + (number%4), "Synthetic contact " + number);
            var student = students.findById(user.getId()).orElseThrow();
            student.getCoursesTaken().add(seededCourses.get((number-1)%seededCourses.size()));
            student.getCoursesTaken().add(seededCourses.get(number%seededCourses.size()));
            if (number%5 != 0) student.setTargetCourse(seededCourses.get((number + 1)%seededCourses.size()));
            student.setPreferredStudyMode(StudyMode.values()[number%StudyMode.values().length]);
            student.setPreferredGroupSizeMin(2);
            student.setPreferredGroupSizeMax(2 + (number%4));
            if (number%6 != 0) student.getStudyGoals().add(StudyGoal.values()[number%StudyGoal.values().length]);
            if (number%7 == 0) student.getStudyGoals().add(StudyGoal.PROBLEM_SOLVING);
            // Includes a few no-availability profiles to exercise honest empty states.
            if (number%8 != 0) {
                int hour = 8 + (number%10);
                availability.save(new AvailabilitySlot(student, DayOfWeek.of(1 + (number%7)), LocalTime.of(hour, 0), LocalTime.of(hour + 2, 0)));
                availability.save(new AvailabilitySlot(student, DayOfWeek.of(1 + ((number + 2)%7)), LocalTime.of(18, 0), LocalTime.of(20, 0)));
            }
        }
    }
}
