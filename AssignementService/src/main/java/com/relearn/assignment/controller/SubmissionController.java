package com.relearn.assignment.controller;

import com.relearn.assignment.dto.SubmissionRequest;
import com.relearn.assignment.dto.SubmissionResponse;
import com.relearn.assignment.service.FileStorageService;
import com.relearn.assignment.service.SubmissionService;
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
 * REST controller for student submission endpoints.
 *
 * Submissions support:
 *  - Text answer only
 *  - File upload only  (saved to C:/Users/HP/Downloads/submissions)
 *  - Both text + file
 *
 * Upload:   POST /api/submissions          (multipart/form-data)
 * Update:   PUT  /api/submissions/{id}     (multipart/form-data)
 * Download: GET  /api/submissions/download/{filename}
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Student assignment submission with file upload support")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {

    private final SubmissionService  submissionService;
    private final FileStorageService fileStorageService;

    // ----------------------------------------------------------------
    //  POST /api/submissions
    //  Submit an assignment (text, file, or both)
    // ----------------------------------------------------------------

    /**
     * Submits an assignment. Accepts multipart/form-data.
     *
     * Form fields:
     *   assignmentId    (required)
     *   studentId       (required)
     *   submissionText  (optional — text answer)
     *   file            (optional — PDF, DOCX, image, etc.)
     *
     * At least one of submissionText or file must be provided.
     *
     * If a file is provided it is saved to C:/Users/HP/Downloads/submissions
     * and the filename is stored in the database.
     *
     * Status is automatically set to LATE if submitted after the deadline.
     * Returns 409 if the student already submitted (use PUT to update).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit an assignment (text and/or file)",
               description = "Accepts multipart/form-data. File saved to C:/Users/HP/Downloads/submissions. " +
                             "Status auto-set to LATE if past deadline.")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @RequestParam("assignmentId")                          Long assignmentId,
            @RequestParam("studentId")                             Long studentId,
            @RequestParam(value = "submissionText", required = false) String submissionText,
            @RequestParam(value = "file",           required = false) MultipartFile file) {

        // Validate: at least one of text or file must be provided
        if ((submissionText == null || submissionText.isBlank())
                && (file == null || file.isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        // Save file to disk if provided
        String storedFilename = null;
        if (file != null && !file.isEmpty()) {
            storedFilename = fileStorageService.storeFile(file);
        }

        SubmissionRequest request = new SubmissionRequest();
        request.setAssignmentId(assignmentId);
        request.setStudentId(studentId);
        request.setSubmissionText(submissionText);
        request.setFileUrl(storedFilename);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.submitAssignment(request));
    }

    // ----------------------------------------------------------------
    //  PUT /api/submissions/{id}
    //  Update a submission before the deadline
    // ----------------------------------------------------------------

    /**
     * Updates an existing submission. Accepts multipart/form-data.
     *
     * Only allowed before the assignment deadline.
     * Only the student who made the submission can update it.
     *
     * If a new file is provided:
     *   - The old file is deleted from disk
     *   - The new file is saved
     *
     * If no file is provided:
     *   - The existing file is kept
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a submission (resubmit before deadline)",
               description = "If a new file is uploaded, the old file is deleted from disk.")
    public ResponseEntity<SubmissionResponse> updateSubmission(
            @PathVariable Long id,
            @RequestParam("studentId")                             Long studentId,
            @RequestParam(value = "submissionText", required = false) String submissionText,
            @RequestParam(value = "file",           required = false) MultipartFile file) {

        // Get the current submission to find the existing filename
        SubmissionResponse existing = submissionService.getSubmissionById(id);
        String storedFilename = existing.getFileUrl(); // keep existing by default

        if (file != null && !file.isEmpty()) {
            // Delete old file from disk
            if (storedFilename != null) {
                fileStorageService.deleteFile(storedFilename);
            }
            // Save new file
            storedFilename = fileStorageService.storeFile(file);
        }

        SubmissionRequest request = new SubmissionRequest();
        request.setAssignmentId(existing.getAssignmentId());
        request.setStudentId(studentId);
        request.setSubmissionText(submissionText);
        request.setFileUrl(storedFilename);

        return ResponseEntity.ok(submissionService.updateSubmission(id, request, studentId));
    }

    // ----------------------------------------------------------------
    //  GET /api/submissions/download/{filename}
    //  Download a submission file
    // ----------------------------------------------------------------

    /**
     * Downloads a submission file by its stored filename.
     *
     * The filename comes from SubmissionResponse.fileUrl.
     * The file is streamed back as an attachment.
     *
     * Example:
     * GET /api/submissions/download/homework_1715000000000.pdf
     */
    @GetMapping("/download/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a submission file",
               description = "Streams the file. filename comes from SubmissionResponse.fileUrl")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFile(filename);

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
    //  Read-only endpoints (unchanged)
    // ----------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get all submissions", description = "TEACHER or ADMIN only.")
    public ResponseEntity<List<SubmissionResponse>> getAllSubmissions() {
        return ResponseEntity.ok(submissionService.getAllSubmissions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get submission by ID")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get submissions for an assignment", description = "TEACHER or ADMIN only.")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByAssignment(assignmentId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all submissions by a student")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByStudent(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByStudent(studentId));
    }

    @GetMapping("/my/{assignmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my submission for an assignment")
    public ResponseEntity<SubmissionResponse> getMySubmission(
            @PathVariable Long assignmentId,
            @RequestParam Long studentId) {
        return ResponseEntity.ok(
                submissionService.getMySubmissionForAssignment(assignmentId, studentId));
    }
}
