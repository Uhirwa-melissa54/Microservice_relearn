package com.relearn.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for returning student count for a class.
 * Used by the teacher dashboard to show "total students" per class.
 */
@Getter
@Setter
@AllArgsConstructor
public class StudentCountResponse {

    private String className;
    private long studentCount;
}
