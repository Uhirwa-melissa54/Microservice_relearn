package com.relearn.notes.controller;

import com.relearn.notes.dto.NoteRequest;
import com.relearn.notes.dto.NoteResponse;
import com.relearn.notes.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for note CRUD operations (primarily teacher-facing).
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Note CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create a note", description = "Upload a new note. TEACHER or ADMIN only.")
    public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody NoteRequest request) {
        NoteResponse response = noteService.createNote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all notes")
    public ResponseEntity<List<NoteResponse>> getAllNotes() {
        return ResponseEntity.ok(noteService.getAllNotes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get note by ID")
    public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    @GetMapping("/class/{className}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notes by class")
    public ResponseEntity<List<NoteResponse>> getNotesByClass(@PathVariable String className) {
        return ResponseEntity.ok(noteService.getNotesByClass(className));
    }

    @GetMapping("/course/{courseName}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notes by course")
    public ResponseEntity<List<NoteResponse>> getNotesByCourse(@PathVariable String courseName) {
        return ResponseEntity.ok(noteService.getNotesByCourse(courseName));
    }

    @GetMapping("/year/{academicYear}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notes by academic year")
    public ResponseEntity<List<NoteResponse>> getNotesByAcademicYear(@PathVariable String academicYear) {
        return ResponseEntity.ok(noteService.getNotesByAcademicYear(academicYear));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a note", description = "TEACHER or ADMIN only.")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(noteService.updateNote(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a note", description = "TEACHER or ADMIN only.")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
