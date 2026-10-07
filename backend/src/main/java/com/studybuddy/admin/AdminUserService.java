package com.studybuddy.admin;

import com.studybuddy.auth.AccountCreation;
import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.InputRules;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserNotFoundException;
import com.studybuddy.user.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUserService {
    private final UserRepository users;
    private final StudentRepository students;
    private final AdminUserAssembler assembler;
    private final UserUsageCounter usage;
    private final StudentDeactivation deactivation;
    private final StudentDeletion deletion;
    private final AccountCreation creation;
    private final AccountAccess access;

    public AdminUserService(UserRepository users, StudentRepository students, AdminUserAssembler assembler, UserUsageCounter usage,
        StudentDeactivation deactivation, StudentDeletion deletion, AccountCreation creation, AccountAccess access) {
        this.users = users;
        this.students = students;
        this.assembler = assembler;
        this.usage = usage;
        this.deactivation = deactivation;
        this.deletion = deletion;
        this.creation = creation;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryDto> list(AdminUserFilter filter, Long actorId) {
        access.requireAdmin(actorId);
        var profiles = students.findAll().stream().collect(Collectors.toMap(Student::getId, Function.identity()));
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(user -> filter.matches(user, profiles.get(user.getId()))).map(user -> assembler.toSummary(user, profiles.get(user.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public AdminAccountsSummaryDto summary(Long actorId) {
        access.requireAdmin(actorId);
        var all = users.findAll();
        long active = all.stream().filter(User::isActive).count();
        long studentCount = all.stream().filter(user -> user.getRole() == Role.STUDENT).count();
        return new AdminAccountsSummaryDto(all.size(), active, all.size() - active, studentCount, all.size() - studentCount);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto get(Long id, Long actorId) {
        access.requireAdmin(actorId);
        return detail(users.findById(id).orElseThrow(() -> new UserNotFoundException(id)));
    }

    public AdminUserDetailDto create(AdminUserCreateRequest request, Long actorId) {
        access.lockAdmin(actorId);
        if (request == null) {
            throw new InvalidAdminUserException("Account details are required");
        }
        return detail(creation.create(request.email(), request.password(), request.role(), request.name(), request.school(),
            request.programme(), request.yearOfStudy(), request.contactNumber()));
    }

    public AdminUserDetailDto update(Long id, AdminUserUpdateRequest request, Long actorId) {
        User user = access.lockAdminTarget(actorId, id);
        if (request == null) {
            throw new InvalidAdminUserException("Account details are required");
        }
        String email = InputRules.email(request.email());
        if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        var student = students.findById(id).orElse(null);
        if (user.getRole() == Role.STUDENT && student == null) {
            throw new UserNotFoundException(id);
        }
        if (student != null) {
            String name = InputRules.required(request.name(), "Name", InputRules.TEXT_LIMIT);
            String school = InputRules.required(request.school(), "School", InputRules.TEXT_LIMIT);
            String programme = InputRules.required(request.programme(), "Programme", InputRules.TEXT_LIMIT);
            InputRules.year(request.yearOfStudy());
            String contact = request.contactNumber() == null ? null : InputRules.required(request.contactNumber(), "Contact number", InputRules.TEXT_LIMIT);
            student.setName(name);
            student.setSchool(school);
            student.setProgramme(programme);
            student.setYearOfStudy(request.yearOfStudy());
            if (contact != null) {
                student.setContactNumber(contact);
            }
        }
        user.setEmail(email);
        return detail(user);
    }

    public AdminUserDetailDto deactivate(Long id, Long actorId) {
        User user = access.lockLifecycle(actorId, id);
        removalAllowed(user, actorId);
        if (!user.isActive()) {
            throw new IllegalStateException("This account is already deactivated");
        }
        user.deactivate();
        if (user.getRole() == Role.STUDENT) {
            deactivation.apply(id);
        }
        return detail(user);
    }

    public AdminUserDetailDto reactivate(Long id, Long actorId) {
        User user = access.lockLifecycle(actorId, id);
        if (user.isActive()) {
            throw new IllegalStateException("This account is already active");
        }
        user.setActive(true);
        return detail(user);
    }

    public void deletePermanently(Long id, Long actorId) {
        User user = access.lockLifecycle(actorId, id);
        removalAllowed(user, actorId);
        if (user.getRole() == Role.STUDENT) {
            deletion.apply(id);
        }
        users.delete(user);
        users.flush();
    }

    private void removalAllowed(User user, Long actorId) {
        if (Objects.equals(user.getId(), actorId)) {
            throw new AdminActionNotAllowedException("You cannot deactivate or delete your own account");
        }
        if (user.getRole() == Role.ADMIN && user.isActive() && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new AdminActionNotAllowedException("The last active administrator cannot be removed");
        }
    }

    private AdminUserDetailDto detail(User user) {
        var student = students.findById(user.getId()).orElse(null);
        return assembler.toDetail(user, student, user.getRole() == Role.STUDENT ? usage.countFor(user.getId()) : null);
    }
}
