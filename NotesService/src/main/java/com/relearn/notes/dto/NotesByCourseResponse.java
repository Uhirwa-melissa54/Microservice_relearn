package com.relearn.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO that groups notes under a single course name.
 * Used by the "get notes grouped by course" endpoint.
 *
 * Example response:
 * {
 *   "courseName": "Mathematics",
 *   "totalNotes": 5,
 *   "notes": [ ... ]
 * }
 */
@Getter
@Setter
@AllArgsConstructor
public class NotesByCourseResponse {

    private String courseName;
    private int totalNotes;
    private List<NoteResponse> notes;
}
