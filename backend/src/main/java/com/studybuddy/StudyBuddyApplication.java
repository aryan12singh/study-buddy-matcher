package com.studybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Entry point of the Spring Boot application. The default in-memory user store is excluded because
 * accounts live in the database and sign-in uses JWTs.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class StudyBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyBuddyApplication.class, args);
    }
}
