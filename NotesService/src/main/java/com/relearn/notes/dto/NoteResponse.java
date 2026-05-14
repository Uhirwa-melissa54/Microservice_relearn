package com.relearn.notes.dto;

import com.relearn.notes.entity.Note;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning note data in API responses.
 *
 * We never expose the entity directly — this gives us control
 * over exactly what fields the client sees, and makes it easy
 * to add/remove fields without breaking the API contract.
 */
@Getter
@Setter
public class NoteResponse {

    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String className;
    private String courseName;
    private String academicYear;
    private Long teacherId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Static factory method — converts a Note entity to a NoteResponse DTO.
     * Keeps all mapping logic in one place.
     *
     * @param note the entity from the database
     * @return a clean DTO safe to return to the client
     */
    public static NoteResponse fromEntity(Note note) {
        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setTitle(note.getTitle());
        response.setDescription(note.getDescription());
        response.setFileUrl(note.getFileUrl());
        response.setClassName(note.getClassName());
        response.setCourseName(note.getCourseName());
        response.setAcademicYear(note.getAcademicYear());
        response.setTeacherId(note.getTeacherId());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
    }
}
