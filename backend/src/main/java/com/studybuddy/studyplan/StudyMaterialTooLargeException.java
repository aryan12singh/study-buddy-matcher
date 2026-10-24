package com.studybuddy.studyplan;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/** An uploaded file is over the configured size, or a text file holds too much text. Returned as 413. */
public class StudyMaterialTooLargeException extends ApiException {
    public StudyMaterialTooLargeException(String message) {
        super(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", message);
    }
}
