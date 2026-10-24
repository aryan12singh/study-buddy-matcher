package com.studybuddy.studyplan;

import com.studybuddy.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * An upload is not a PDF, {@code .txt} or {@code .md} file, or its contents do not match its
 * extension, such as a renamed Word document. Returned as 415.
 */
public class UnsupportedStudyMaterialException extends ApiException {
    public UnsupportedStudyMaterialException() {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
                "Upload a PDF, .txt or .md file. Export Word or PowerPoint files to PDF first");
    }
}
