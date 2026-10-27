package com.studybuddy.student;

import com.studybuddy.common.error.InvalidInputException;
import java.util.Arrays;
import java.util.Locale;

/** The app is for SMU students, so a student's school is one of SMU's schools, stored by its code. */
public enum SmuSchool {
    SOA("School of Accountancy"),
    LKCSB("Lee Kong Chian School of Business"),
    SOE("School of Economics"),
    SCIS("School of Computing and Information Systems"),
    YPHSL("Yong Pung How School of Law"),
    SOSS("School of Social Sciences"),
    CIS("College of Integrative Studies");

    private final String displayName;

    SmuSchool(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Returns the canonical code for a school code in any case, or rejects anything else. */
    public static String code(String value) {
        String normal = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(school -> school.name().equals(normal))
            .findFirst()
            .map(Enum::name)
            .orElseThrow(() -> new InvalidInputException("school", "Choose one of SMU's schools"));
    }
}
