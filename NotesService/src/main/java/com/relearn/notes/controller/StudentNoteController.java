package com.relearn.notes.controller;

import com.relearn.notes.dto.NoteResponse;
import com.relearn.notes.dto.NotesByCourseResponse;
import com.relearn.notes.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-facing notes endpoints.
 *
 * These endpoints are optimized for what the frontend needs:
 *  - Notes grouped by course (main notes view)
 *  - Recent notes (dashboard widget)
 *  - Notes by academic year (history)
 *  - Note detail (view/download)
 *
 * All endpoints require authentication (any role).
 */
@RestController
@RequestMapping("/api/student/notes")
@RequiredArgsConstructor
@Tag(name = "Student - Notes", description = "Student note viewing and filtering endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StudentNoteController {

    private final NoteService noteService;

    /**
     * GET /api/student/notes/class/{className}
     *
     * Returns all notes for a class, grouped by course.
     * This is the main notes view for students.
     *
     * Optional query param: academicYear (e.g. "2024-2025")
     * If omitted, returns notes from all years.
     *
     * Example response:
     * [
     *   {
     *     "courseName": "Mathematics",
     *     "totalNotes": 3,
     *     "notes": [ { "id": 1, "title": "Chapter 1", ... }, ... ]
     *   },
     *   {
     *     "courseName": "Physics",
     *     "totalNotes": 2,
     *     "notes": [ ... ]
     *   }
     * ]
     */
    @GetMapping("/class/{className}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notes grouped by course",
               description = "Returns all notes for a class grouped by course name. " +
                             "Optionally filter by academic year.")
    public ResponseEntity<List<NotesByCourseResponse>> getNotesByClassGroupedByCourse(
            @PathVariable String className,
            @Parameter(description = "Filter by academic year, e.g. 2024-2025")
            @RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(noteService.getNotesGroupedByCourse(className, academicYear));
    }

    /**
     * GET /api/student/notes/{id}
     *
     * Returns full details of a single note.
     * Students use this to view the note description, who uploaded it, and get the download URL.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get note details",
               description = "Returns full note details including description, teacher ID, file URL, and upload date.")
    public ResponseEntity<NoteResponse> getNoteDetail(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    /**
     * GET /api/student/notes/recent/{className}
     *
     * Returns the most recently uploaded notes for a class.
     * Used to populate the "recent notes" widget on the student dashboard.
     *
     * Query param: limit (default 5, max 20)
     */
    @GetMapping("/recent/{className}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recent notes",
               description = "Returns the N most recently uploaded notes for a class. Default limit is 5.")
    public ResponseEntity<List<NoteResponse>> getRecentNotes(
            @PathVariable String className,
            @Parameter(description = "Number of recent notes to return (default 5)")
            @RequestParam(defaultValue = "5") int limit) {
        // Cap at 20 to prevent abuse
        int safeLimit = Math.min(limit, 20);
        return ResponseEntity.ok(noteService.getRecentNotesByClass(className, safeLimit));
    }

    /**
     * GET /api/student/notes/history/{className}/{academicYear}
     *
     * Returns all notes for a class in a specific academic year, grouped by course.
     * Used for the academic history feature.
     *
     * Example: GET /api/student/notes/history/Y1A/2023-2024
     */
    @GetMapping("/history/{className}/{academicYear}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notes by academic year (history)",
               description = "Returns all notes for a class in a specific academic year, grouped by course.")
    public ResponseEntity<List<NotesByCourseResponse>> getNotesByClassAndYear(
            @PathVariable String className,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(noteService.getNotesGroupedByCourse(className, academicYear));
    }

    /**
     * GET /api/student/notes/course/{courseName}
     *
     * Returns all notes for a specific course across all classes.
     * Students browsing other classes use this.
     */
    @GetMapping("/course/{courseName}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notes by course",
               description = "Returns all notes for a specific course. Useful when browsing other classes.")
    public ResponseEntity<List<NoteResponse>> getNotesByCourse(@PathVariable String courseName) {
        return ResponseEntity.ok(noteService.getNotesByCourse(courseName));
    }
}
