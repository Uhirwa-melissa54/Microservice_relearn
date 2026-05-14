package com.relearn.assignment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a student tries to submit an assignment they have already submitted.
 * Automatically maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateSubmissionException extends RuntimeException {

    public DuplicateSubmissionException(String message) {
        super(message);
    }
}
