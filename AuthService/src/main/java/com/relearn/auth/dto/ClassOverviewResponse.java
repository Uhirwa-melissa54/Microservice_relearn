package com.relearn.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for a class overview card on the admin dashboard.
 *
 * A "class" in Relearn is identified by its className string (e.g. "Y1A").
 * There is no separate Class entity — classes are derived from user.className values.
 *
 * Example:
 * {
 *   "className": "Y1A",
 *   "academicYear": "2024-2025",
 *   "totalStudents": 35,
 *   "teachers": ["John Doe (Mathematics)", "Jane Smith (Physics)"],
 *   "totalAssignments": 24,
 *   "totalNotes": 48
 * }
 */
@Getter
@Setter
@Builder
public class ClassOverviewResponse {

    private String className;
    private String academicYear;
    private Integer capacity;
    private boolean active;
    private long totalStudents;
    private Long teacherId;
    private String teacherName;

    private long totalAssignments;
    private long totalNotes;
}
