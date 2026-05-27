package com.relearn.notes.controller;

import com.relearn.notes.dto.NoteResponse;
import com.relearn.notes.service.FileStorageService;
import com.relearn.notes.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Teacher-facing notes endpoints with real file upload/download.
 *
 * Files are stored at: C:/Users/HP/Downloads/notes
 * The stored filename is saved in the database as Note.fileUrl.
 *
 * Upload:   POST   /api/teacher/notes          (multipart/form-data)
 * Update:   PUT    /api/teacher/notes/{id}     (multipart/form-data)
 * Download: GET    /api/teacher/notes/download/{filename}
 * Delete:   DELETE /api/teacher/notes/{id}
 */
@RestController
@RequestMapping("/api/teacher/notes")
@RequiredArgsConstructor
@Tag(name = "Teacher - Notes", description = "Teacher note upload, management, and download")
@SecurityRequirement(name = "bearerAuth")
public class TeacherNoteController {

    private final NoteService        noteService;
    private final FileStorageService fileStorageService;

    // ----------------------------------------------------------------
    //  GET /api/teacher/notes/{teacherId}
    // ----------------------------------------------------------------

    @GetMapping("/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get teacher's notes")
    public ResponseEntity<List<NoteResponse>> getTeacherNotes(@PathVariable Long teacherId) {
        return ResponseEntity.ok(noteService.getNotesByTeacher(teacherId));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/notes/{teacherId}/class/{className}/course/{courseName}
    // ----------------------------------------------------------------

    @GetMapping("/{teacherId}/class/{className}/course/{courseName}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get notes by class and course")
    public ResponseEntity<List<NoteResponse>> getNotesByClassAndCourse(
            @PathVariable Long teacherId,
            @PathVariable String className,
            @PathVariable String courseName) {
        return ResponseEntity.ok(
                noteService.getNotesByTeacherAndClassAndCourse(teacherId, className, courseName));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/notes/detail/{id}
    // ----------------------------------------------------------------

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get note details (for edit form)")
    public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    // ----------------------------------------------------------------
    //  POST /api/teacher/notes
    //  Upload a new note with an optional file
    // ----------------------------------------------------------------

    /**
     * Creates a new note. Accepts multipart/form-data.
     *
     * Form fields:
     *   title        (required)
     *   description  (optional)
     *   className    (required)
     *   courseName   (required)
     *   academicYear (required)
     *   teacherId    (required)
     *   file         (optional — PDF, DOCX, etc.)
     *
     * If a file is provided it is saved to C:/Users/HP/Downloads/notes
     * and the filename is stored in the database.
     * If no file is provided, fileUrl is left null.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Upload a note with optional file",
               description = "Accepts multipart/form-data. File is saved to C:/Users/HP/Downloads/notes")
    public ResponseEntity<NoteResponse> uploadNote(
            @RequestParam("title")        String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("className")    String className,
            @RequestParam("courseName")   String courseName,
            @RequestParam("academicYear") String academicYear,
            @RequestParam("teacherId")    Long teacherId,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // Save the file to disk if provided
        String storedFilename = null;
        if (file != null && !file.isEmpty()) {
            storedFilename = fileStorageService.storeFile(file);
        }

        // Build the NoteRequest manually (no JSON body — this is multipart)
        com.relearn.notes.dto.NoteRequest request = new com.relearn.notes.dto.NoteRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setClassName(className);
        request.setCourseName(courseName);
        request.setAcademicYear(academicYear);
        request.setTeacherId(teacherId);
        request.setFileUrl(storedFilename);   // stored filename, not a URL

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.createNote(request));
    }

    // ----------------------------------------------------------------
    //  PUT /api/teacher/notes/{id}
    //  Update a note — optionally replace the file
    // ----------------------------------------------------------------

    /**
     * Updates an existing note. Accepts multipart/form-data.
     *
     * If a new file is provided:
     *   - The old file is deleted from disk
     *   - The new file is saved and its filename stored in the DB
     *
     * If no file is provided:
     *   - The existing file is kept unchanged
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a note (optionally replace file)",
               description = "If a new file is uploaded, the old file is deleted from disk.")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @RequestParam("title")        String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("className")    String className,
            @RequestParam("courseName")   String courseName,
            @RequestParam("academicYear") String academicYear,
            @RequestParam("teacherId")    Long teacherId,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // Get the current note to find the existing filename
        NoteResponse existing = noteService.getNoteById(id);
        String storedFilename = existing.getFileUrl(); // keep existing by default

        if (file != null && !file.isEmpty()) {
            // Delete the old file from disk
            if (storedFilename != null) {
                fileStorageService.deleteFile(storedFilename);
            }
            // Save the new file
            storedFilename = fileStorageService.storeFile(file);
        }

        com.relearn.notes.dto.NoteRequest request = new com.relearn.notes.dto.NoteRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setClassName(className);
        request.setCourseName(courseName);
        request.setAcademicYear(academicYear);
        request.setTeacherId(teacherId);
        request.setFileUrl(storedFilename);

        return ResponseEntity.ok(noteService.updateNote(id, request));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/notes/download/{filename}
    //  Download / stream a note file
    // ----------------------------------------------------------------

    /**
     * Downloads a note file by its stored filename.
     *
     * The filename comes from NoteResponse.fileUrl.
     * The file is streamed back as an attachment.
     *
     * Example:
     * GET /api/teacher/notes/download/math-ch3_1715000000000.pdf
     */
    @GetMapping("/download/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a note file",
               description = "Streams the file. filename comes from NoteResponse.fileUrl")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFile(filename);

        // Determine content type
        String contentType = "application/octet-stream";
        try {
            contentType = resource.getFile().toURI().toURL().openConnection().getContentType();
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // ----------------------------------------------------------------
    //  DELETE /api/teacher/notes/{id}
    // ----------------------------------------------------------------

    /**
     * Deletes a note and its associated file from disk.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a note (also deletes the file from disk)")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        // Get the filename before deleting the DB record
        NoteResponse note = noteService.getNoteById(id);
        if (note.getFileUrl() != null) {
            fileStorageService.deleteFile(note.getFileUrl());
        }
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
