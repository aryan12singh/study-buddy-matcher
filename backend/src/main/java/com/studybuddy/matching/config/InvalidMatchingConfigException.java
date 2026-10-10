package com.studybuddy.matching.config;

import com.studybuddy.common.error.InvalidInputException;
/** Thrown when the matching settings an admin supplies are missing or invalid. */
public class InvalidMatchingConfigException extends InvalidInputException {

    public InvalidMatchingConfigException(String message) {
        super(message);
    }
}
