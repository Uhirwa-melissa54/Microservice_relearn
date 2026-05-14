package com.relearn.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO that groups assignments under a single course name.
 * Used by the "get assignments grouped by course" endpoint.
 *
 * Example response:
 * {
 *   "courseName": "Mathematics",
 *   "totalAssignments": 3,
 *   "assignments": [ ... ]
 * }
 */
@Getter
@Setter
@AllArgsConstructor
public class AssignmentsByCourseResponse {

    private String courseName;
    private int totalAssignments;
    private List<AssignmentResponse> assignments;
}
