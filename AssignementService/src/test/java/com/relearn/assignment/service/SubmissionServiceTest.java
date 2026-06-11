package com.relearn.assignment.service;

import com.relearn.assignment.dto.GradeSubmissionRequest;
import com.relearn.assignment.dto.SubmissionRequest;
import com.relearn.assignment.dto.SubmissionResponse;
import com.relearn.assignment.entity.Assignment;
import com.relearn.assignment.entity.Submission;
import com.relearn.assignment.enums.SubmissionStatus;
import com.relearn.assignment.enums.SubmissionType;
import com.relearn.assignment.exception.DuplicateSubmissionException;
import com.relearn.assignment.exception.ResourceNotFoundException;
import com.relearn.assignment.repository.AssignmentRepository;
import com.relearn.assignment.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService unit tests")
class SubmissionServiceTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock AssignmentRepository assignmentRepository;
    @InjectMocks SubmissionService submissionService;

    private Assignment activeAssignment;
    private Assignment overdueAssignment;

    @BeforeEach
    void setUp() {
        activeAssignment = Assignment.builder()
                .id(1L).title("Calculus PS3").className("Y1A")
                .courseName("Mathematics").teacherId(10L)
                .deadline(LocalDateTime.now().plusDays(7))
                .submissionType(SubmissionType.BOTH)
                .build();

        overdueAssignment = Assignment.builder()
                .id(2L).title("Old Assignment").className("Y1A")
                .courseName("Mathematics").teacherId(10L)
                .deadline(LocalDateTime.now().minusDays(1))
                .submissionType(SubmissionType.BOTH)
                .build();
    }

    // ----------------------------------------------------------------
    //  submitAssignment — on-time
    // ----------------------------------------------------------------

    @Test
    @DisplayName("submitAssignment: status is PENDING when submitted before deadline")
    void submitAssignment_statusPendingWhenOnTime() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(activeAssignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 5L)).thenReturn(Optional.empty());

        Submission saved = Submission.builder()
                .id(1L).assignment(activeAssignment).studentId(5L)
                .submissionText("My answer").status(SubmissionStatus.PENDING)
                .build();
        when(submissionRepository.save(any())).thenReturn(saved);

        SubmissionRequest req = new SubmissionRequest();
        req.setAssignmentId(1L);
        req.setStudentId(5L);
        req.setSubmissionText("My answer");

        SubmissionResponse response = submissionService.submitAssignment(req);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(submissionRepository).save(any(Submission.class));
    }

    // ----------------------------------------------------------------
    //  submitAssignment — late
    // ----------------------------------------------------------------

    @Test
    @DisplayName("submitAssignment: status is LATE when submitted after deadline")
    void submitAssignment_statusLateWhenOverdue() {
        when(assignmentRepository.findById(2L)).thenReturn(Optional.of(overdueAssignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(2L, 5L)).thenReturn(Optional.empty());

        Submission saved = Submission.builder()
                .id(2L).assignment(overdueAssignment).studentId(5L)
                .submissionText("Late answer").status(SubmissionStatus.LATE)
                .build();
        when(submissionRepository.save(any())).thenReturn(saved);

        SubmissionRequest req = new SubmissionRequest();
        req.setAssignmentId(2L);
        req.setStudentId(5L);
        req.setSubmissionText("Late answer");

        SubmissionResponse response = submissionService.submitAssignment(req);

        assertThat(response.getStatus()).isEqualTo("LATE");
    }

    // ----------------------------------------------------------------
    //  submitAssignment — duplicate prevention
    // ----------------------------------------------------------------

    @Test
    @DisplayName("submitAssignment: throws DuplicateSubmissionException when already submitted")
    void submitAssignment_throwsOnDuplicate() {
        Submission existing = Submission.builder()
                .id(1L).assignment(activeAssignment).studentId(5L)
                .status(SubmissionStatus.PENDING).build();

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(activeAssignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 5L))
                .thenReturn(Optional.of(existing));

        SubmissionRequest req = new SubmissionRequest();
        req.setAssignmentId(1L);
        req.setStudentId(5L);
        req.setSubmissionText("Duplicate");

        assertThatThrownBy(() -> submissionService.submitAssignment(req))
                .isInstanceOf(DuplicateSubmissionException.class);
    }

    // ----------------------------------------------------------------
    //  submitAssignment — assignment not found
    // ----------------------------------------------------------------

    @Test
    @DisplayName("submitAssignment: throws ResourceNotFoundException when assignment missing")
    void submitAssignment_throwsWhenAssignmentNotFound() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());

        SubmissionRequest req = new SubmissionRequest();
        req.setAssignmentId(99L);
        req.setStudentId(5L);

        assertThatThrownBy(() -> submissionService.submitAssignment(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  gradeSubmission
    // ----------------------------------------------------------------

    @Test
    @DisplayName("gradeSubmission: sets status GRADED and records score/feedback")
    void gradeSubmission_setsGradedStatus() {
        Submission sub = Submission.builder()
                .id(1L).assignment(activeAssignment).studentId(5L)
                .status(SubmissionStatus.PENDING).build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenReturn(sub);

        GradeSubmissionRequest req = new GradeSubmissionRequest();
        req.setScore(88.0);
        req.setMaxScore(100.0);
        req.setFeedback("Well done!");
        req.setGradedBy(10L);

        SubmissionResponse response = submissionService.gradeSubmission(1L, req, 10L);

        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(sub.getScore()).isEqualTo(88.0);
        assertThat(sub.getFeedback()).isEqualTo("Well done!");
        assertThat(sub.getGradedBy()).isEqualTo(10L);
        assertThat(sub.getGradedAt()).isNotNull();
    }

    @Test
    @DisplayName("gradeSubmission: throws when score exceeds maxScore")
    void gradeSubmission_throwsWhenScoreExceedsMax() {
        Submission sub = Submission.builder()
                .id(1L).assignment(activeAssignment).studentId(5L)
                .status(SubmissionStatus.PENDING).build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));

        GradeSubmissionRequest req = new GradeSubmissionRequest();
        req.setScore(110.0);
        req.setMaxScore(100.0);
        req.setGradedBy(10L);

        assertThatThrownBy(() -> submissionService.gradeSubmission(1L, req, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    @DisplayName("gradeSubmission: throws when teacher doesn't own the assignment")
    void gradeSubmission_throwsWhenNotOwner() {
        Submission sub = Submission.builder()
                .id(1L).assignment(activeAssignment).studentId(5L)
                .status(SubmissionStatus.PENDING).build();
        // activeAssignment.teacherId = 10, but grading teacher is 99

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));

        GradeSubmissionRequest req = new GradeSubmissionRequest();
        req.setScore(80.0);
        req.setMaxScore(100.0);
        req.setGradedBy(99L);

        assertThatThrownBy(() -> submissionService.gradeSubmission(1L, req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own assignments");
    }

    // ----------------------------------------------------------------
    //  getSubmissionById
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getSubmissionById: returns submission when found")
    void getSubmissionById_returnsSubmission() {
        Submission sub = Submission.builder()
                .id(1L).assignment(activeAssignment).studentId(5L)
                .status(SubmissionStatus.PENDING).build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionResponse response = submissionService.getSubmissionById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStudentId()).isEqualTo(5L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getSubmissionById: throws when not found")
    void getSubmissionById_throwsWhenNotFound() {
        when(submissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getSubmissionById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
