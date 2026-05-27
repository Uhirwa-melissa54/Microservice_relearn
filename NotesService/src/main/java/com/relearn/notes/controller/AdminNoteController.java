package com.relearn.notes.controller;

import com.relearn.notes.dto.AdminNoteStatsResponse;
import com.relearn.notes.dto.NoteResponse;
import com.relearn.notes.repository.NoteRepository;
import com.relearn.notes.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-facing notes endpoints.
 *
 * Provides system-wide statistics and full visibility into all notes —
 * not scoped to any single teacher or class.
 *
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Notes", description = "System-wide notes statistics and management")
@SecurityRequirement(name = "bearerAuth")
public class AdminNoteController {

    private final NoteRepository noteRepository;
    private final NoteService    noteService;

    // ----------------------------------------------------------------
    //  GET /api/admin/notes/stats
    //  System-wide notes statistics
    // ----------------------------------------------------------------

    /**
     * Returns system-wide notes statistics.
     * Used by the admin dashboard to show notes activity.
     *
     * Response:
     * {
     *   "totalNotes": 480,
     *   "classesWithNotes": 12,
     *   "coursesWithNotes": 8,
     *   "activeTeachers": 18
     * }
     */
    @GetMapping("/stats")
    @Operation(summary = "Get system-wide notes statistics",
               description = "Returns total notes, distinct classes, courses, and active teachers.")
    public ResponseEntity<AdminNoteStatsResponse> getStats() {
        long totalNotes      = noteRepository.count();
        long classesWithNotes  = noteRepository.findDistinctClassNames().size();
        long coursesWithNotes  = noteRepository.findDistinctCourseNames().size();
        long activeTeachers    = noteRepository.findDistinctTeacherIds().size();

        return ResponseEntity.ok(AdminNoteStatsResponse.builder()
                .totalNotes(totalNotes)
                .classesWithNotes(classesWithNotes)
                .coursesWithNotes(coursesWithNotes)
                .activeTeachers(activeTeachers)
                .build());
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/notes
    //  All notes system-wide
    // ----------------------------------------------------------------

    /**
     * Returns all notes in the system.
     * Admin can see every note regardless of teacher.
     */
    @GetMapping
    @Operation(summary = "Get all notes (admin view)",
               description = "Returns all notes system-wide.")
    public ResponseEntity<List<NoteResponse>> getAllNotes() {
        return ResponseEntity.ok(noteService.getAllNotes());
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/notes/class/{className}
    //  All notes for a class
    // ----------------------------------------------------------------

    /**
     * Returns all notes for a specific class.
     * Used on the admin class details page.
     */
    @GetMapping("/class/{className}")
    @Operation(summary = "Get notes for a class")
    public ResponseEntity<List<NoteResponse>> getNotesByClass(
            @PathVariable String className) {
        return ResponseEntity.ok(noteService.getNotesByClass(className));
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/notes/{id}
    //  Single note details
    // ----------------------------------------------------------------

    /**
     * Returns full details of a single note.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get note by ID")
    public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    // ----------------------------------------------------------------
    //  DELETE /api/admin/notes/{id}
    //  Admin force-delete a note
    // ----------------------------------------------------------------

    /**
     * Admin permanently deletes a note.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a note (admin)",
               description = "Permanently deletes a note.")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
