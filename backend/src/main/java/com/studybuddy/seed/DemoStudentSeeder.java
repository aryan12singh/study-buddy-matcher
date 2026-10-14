package com.studybuddy.seed;

import com.studybuddy.auth.AccountCreation;
import com.studybuddy.seed.DemoData.StudentEntry;
import com.studybuddy.student.ProfileService;
import com.studybuddy.student.ReplaceAvailabilityRequest;
import com.studybuddy.student.UpdateProfileRequest;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates the demo students through the same rules as registration and the profile screen,
 * so seeded profiles are exactly what a student could have saved. Existing accounts are
 * never changed.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoStudentSeeder {

    static final String EMAIL_DOMAIN = "@demo.example.test";

    private final AccountCreation creation;
    private final UserRepository users;
    private final ProfileService profiles;

    public DemoStudentSeeder(AccountCreation creation, UserRepository users, ProfileService profiles) {
        this.creation = creation;
        this.users = users;
        this.profiles = profiles;
    }

    public DemoRoster seed(DemoData data, Map<String, Long> courseIds, String password) {
        Map<String, Long> ids = new LinkedHashMap<>();
        Set<String> created = new HashSet<>();
        for (StudentEntry student : data.students()) {
            Optional<User> existing = users.findByEmail(student.handle() + EMAIL_DOMAIN);
            if (existing.isPresent()) {
                ids.put(student.handle(), existing.get().getId());
                continue;
            }
            ids.put(student.handle(), create(student, data, courseIds, password));
            created.add(student.handle());
        }
        return new DemoRoster(ids, created);
    }

    private Long create(StudentEntry student, DemoData data, Map<String, Long> courseIds, String password) {
        User user = creation.create(student.handle() + EMAIL_DOMAIN, password, Role.STUDENT, student.name(),
                student.school(), student.programme(), student.year(), student.contactNumber());
        profiles.updateMine(user.getId(), new UpdateProfileRequest(student.name(), student.school(),
                student.programme(), student.year(), student.contactNumber(), courseIdsOf(student.courses(), courseIds),
                courseIdOf(student.targetCourse(), courseIds), student.studyMode(), student.groupSize(), student.goals()));
        profiles.replaceAvailability(user.getId(), new ReplaceAvailabilityRequest(data.studentSlots(student.availability())));
        return user.getId();
    }

    private static List<Long> courseIdsOf(List<String> codes, Map<String, Long> courseIds) {
        return codes.stream().map(code -> courseIdOf(code, courseIds)).toList();
    }

    static Long courseIdOf(String code, Map<String, Long> courseIds) {
        Long id = courseIds.get(code);
        if (id == null) {
            throw new IllegalStateException("Demo data refers to unknown course " + code);
        }
        return id;
    }
}
