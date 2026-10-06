package com.studybuddy.auth;

import com.studybuddy.admin.DuplicateEmailException;
import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Internal, shared atomic creation policy for registration, admins and demo data. */
@Component
public class AccountCreation {
    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwords;

    public AccountCreation(UserRepository users, StudentRepository students, PasswordEncoder passwords) {
        this.users = users;
        this.students = students;
        this.passwords = passwords;
    }

    public User create(String email, String password, Role role, String name, String school,
        String programme, Integer yearOfStudy, String contactNumber) {
        String identity = InputRules.email(email);
        InputRules.password(password);
        if (role == null) {
            throw new InvalidInputException("role", "Role is required");
        }
        if (users.existsByEmail(identity)) {
            throw new DuplicateEmailException(identity);
        }
        if (role == Role.STUDENT) {
            name = InputRules.required(name, "Name", InputRules.TEXT_LIMIT);
            school = InputRules.required(school, "School", InputRules.TEXT_LIMIT);
            programme = InputRules.required(programme, "Programme", InputRules.TEXT_LIMIT);
            contactNumber = InputRules.required(contactNumber, "Contact number", InputRules.TEXT_LIMIT);
            InputRules.year(yearOfStudy);
        }
        User user = users.saveAndFlush(new User(identity, passwords.encode(password), role));
        if (role == Role.STUDENT) {
            students.saveAndFlush(new Student(user, name, school, programme, yearOfStudy, contactNumber));
        }
        return user;
    }
}
