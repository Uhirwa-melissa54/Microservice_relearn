package com.relearn.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for teacher notes stats on a specific class+course.
 * Used on the teacher's class details page.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class TeacherNoteStatsResponse {

    private String className;
    private String courseName;
    private long totalNotes;
    private List<NoteResponse> notes;
}
