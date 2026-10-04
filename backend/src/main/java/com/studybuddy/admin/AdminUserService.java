package com.studybuddy.admin;

import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin account management: listing, viewing, editing, deactivating and
 * reactivating accounts. The acting admin's id is passed in explicitly until
 * authentication supplies it. Changes to a loaded user need no explicit save:
 * the transaction flushes them on commit.
 *
 * <p>TODO: create(...) is blocked until Team B provides a PasswordEncoder bean.
 */
@Service
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AdminUserAssembler adminUserAssembler;
    private final UserUsageCounter userUsageCounter;
    private final StudentDeactivation studentDeactivation;

    public AdminUserService(UserRepository userRepository,
                            StudentRepository studentRepository,
                            AdminUserAssembler adminUserAssembler,
                            UserUsageCounter userUsageCounter,
                            StudentDeactivation studentDeactivation) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.adminUserAssembler = adminUserAssembler;
        this.userUsageCounter = userUsageCounter;
        this.studentDeactivation = studentDeactivation;
    }

    /** Every account matching the filter, newest first, active or not. */
    @Transactional(readOnly = true)
    public List<AdminUserSummaryDto> list(AdminUserFilter filter) {
        Map<Long, Student> studentsById = studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> filter.matches(user, studentsById.get(user.getId())))
                .map(user -> adminUserAssembler.toSummary(user, studentsById.get(user.getId())))
                .toList();
    }

    /**
     * @throws UserNotFoundException if the account does not exist
     */
    @Transactional(readOnly = true)
    public AdminUserDetailDto get(Long userId) {
        return toDetail(findUser(userId));
    }

    /**
     * Changes the email and, for a student account, the profile fields.
     *
     * @throws UserNotFoundException if the account does not exist
     * @throws InvalidAdminUserException if a required field is missing or invalid
     * @throws DuplicateEmailException if another account already uses the email
     */
    public AdminUserDetailDto update(Long userId, AdminUserUpdateRequest request) {
        User user = findUser(userId);
        Student student = studentRepository.findById(userId).orElse(null);
        validate(request, student != null);

        String email = request.email().strip();
        if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        user.setEmail(email);
        if (student != null) {
            student.setName(request.name().strip());
            student.setSchool(request.school().strip());
            student.setProgramme(request.programme().strip());
            student.setYearOfStudy(request.yearOfStudy());
            student.setContactNumber(request.contactNumber().strip());
        }
        return adminUserAssembler.toDetail(user, student, usageOf(user));
    }

    /**
     * Deactivates the account in place of deleting it. For a student this
     * also applies {@link StudentDeactivation}.
     *
     * @throws AdminActionNotAllowedException if the admin targets their own account
     * @throws UserNotFoundException if the account does not exist
     * @throws IllegalStateException if the account is already deactivated
     */
    public AdminUserDetailDto deactivate(Long userId, Long adminId) {
        if (Objects.equals(userId, adminId)) {
            throw new AdminActionNotAllowedException("You cannot deactivate your own account");
        }
        User user = findUser(userId);
        if (!user.isActive()) {
            throw new IllegalStateException("User " + userId + " is already deactivated");
        }
        user.setActive(false);
        if (user.getRole() == Role.STUDENT) {
            studentDeactivation.apply(userId);
        }
        return toDetail(user);
    }

    /**
     * Lets the account sign in again. Connections and group memberships ended
     * by deactivation are not restored.
     *
     * @throws UserNotFoundException if the account does not exist
     * @throws IllegalStateException if the account is already active
     */
    public AdminUserDetailDto reactivate(Long userId) {
        User user = findUser(userId);
        if (user.isActive()) {
            throw new IllegalStateException("User " + userId + " is already active");
        }
        user.setActive(true);
        return toDetail(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AdminUserDetailDto toDetail(User user) {
        Student student = studentRepository.findById(user.getId()).orElse(null);
        return adminUserAssembler.toDetail(user, student, usageOf(user));
    }

    private UserUsageDto usageOf(User user) {
        return user.getRole() == Role.STUDENT ? userUsageCounter.countFor(user.getId()) : null;
    }

    /**
     * The same rule as the bean validation on {@link AdminUserUpdateRequest},
     * plus the student fields, which are required only for student accounts.
     */
    private static void validate(AdminUserUpdateRequest request, boolean isStudent) {
        if (isBlank(request.email()) || !request.email().contains("@")) {
            throw new InvalidAdminUserException("A valid email is required");
        }
        if (!isStudent) {
            return;
        }
        if (isBlank(request.name()) || isBlank(request.school()) || isBlank(request.programme())
                || isBlank(request.contactNumber())) {
            throw new InvalidAdminUserException(
                    "A student needs a name, school, programme and contact number");
        }
        if (request.yearOfStudy() == null || request.yearOfStudy() < AdminUserUpdateRequest.MIN_YEAR_OF_STUDY) {
            throw new InvalidAdminUserException(
                    "Year of study must be at least " + AdminUserUpdateRequest.MIN_YEAR_OF_STUDY);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
