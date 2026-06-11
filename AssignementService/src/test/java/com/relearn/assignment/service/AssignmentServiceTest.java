package com.relearn.assignment.service;

import com.relearn.assignment.client.UserCountClient;
import com.relearn.assignment.dto.AssignmentRequest;
import com.relearn.assignment.dto.AssignmentResponse;
import com.relearn.assignment.entity.Assignment;
import com.relearn.assignment.enums.SubmissionStatus;
import com.relearn.assignment.enums.SubmissionType;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService unit tests")
class AssignmentServiceTest {

    @Mock AssignmentRepository assignmentRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock UserCountClient      userCountClient;

    @InjectMocks AssignmentService assignmentService;

    private Assignment sampleAssignment;

    @BeforeEach
    void setUp() {
        sampleAssignment = Assignment.builder()
                .id(1L)
                .title("Calculus Problem Set 3")
                .description("Solve all problems in Chapter 3")
                .deadline(LocalDateTime.now().plusDays(7))
                .className("Y1A")
                .courseName("Mathematics")
                .academicYear("2024-2025")
                .teacherId(10L)
                .submissionType(SubmissionType.BOTH)
                .build();
    }

    // ----------------------------------------------------------------
    //  createAssignment
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createAssignment: saves and returns response with ACTIVE status")
    void createAssignment_savesAndReturnsActiveStatus() {
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(sampleAssignment);

        AssignmentRequest req = new AssignmentRequest();
        req.setTitle("Calculus Problem Set 3");
        req.setDescription("Solve all problems in Chapter 3");
        req.setDeadline(LocalDateTime.now().plusDays(7));
        req.setClassName("Y1A");
        req.setCourseName("Mathematics");
        req.setAcademicYear("2024-2025");
        req.setTeacherId(10L);
        req.setSubmissionType(SubmissionType.BOTH);

        AssignmentResponse response = assignmentService.createAssignment(req);

        assertThat(response.getTitle()).isEqualTo("Calculus Problem Set 3");
        assertThat(response.getClassName()).isEqualTo("Y1A");
        assertThat(response.getAssignmentStatus()).isEqualTo("ACTIVE");
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("createAssignment: defaults submissionType to BOTH when null")
    void createAssignment_defaultsSubmissionTypeToBoth() {
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(sampleAssignment);

        AssignmentRequest req = new AssignmentRequest();
        req.setTitle("Test");
        req.setDeadline(LocalDateTime.now().plusDays(1));
        req.setClassName("Y1A");
        req.setCourseName("Math");
        req.setTeacherId(10L);
        req.setSubmissionType(null); // intentionally null

        assignmentService.createAssignment(req);

        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getSubmissionType()).isEqualTo(SubmissionType.BOTH);
    }

    // ----------------------------------------------------------------
    //  getAssignmentById
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAssignmentById: returns assignment when found")
    void getAssignmentById_returnsAssignment() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssignment));

        AssignmentResponse response = assignmentService.getAssignmentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCourseName()).isEqualTo("Mathematics");
    }

    @Test
    @DisplayName("getAssignmentById: throws ResourceNotFoundException when not found")
    void getAssignmentById_throwsWhenNotFound() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ----------------------------------------------------------------
    //  getAssignmentsByClass
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAssignmentsByClass: filters by className")
    void getAssignmentsByClass_filtersCorrectly() {
        when(assignmentRepository.findByClassName("Y1A")).thenReturn(List.of(sampleAssignment));

        List<AssignmentResponse> responses = assignmentService.getAssignmentsByClass("Y1A");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getClassName()).isEqualTo("Y1A");
    }

    // ----------------------------------------------------------------
    //  updateAssignment
    // ----------------------------------------------------------------

    @Test
    @DisplayName("updateAssignment: updates all fields")
    void updateAssignment_updatesFields() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssignment));
        when(assignmentRepository.save(any())).thenReturn(sampleAssignment);

        AssignmentRequest updateReq = new AssignmentRequest();
        updateReq.setTitle("Updated Title");
        updateReq.setDescription("Updated desc");
        updateReq.setDeadline(LocalDateTime.now().plusDays(14));
        updateReq.setClassName("Y1A");
        updateReq.setCourseName("Mathematics");
        updateReq.setTeacherId(10L);
        updateReq.setSubmissionType(SubmissionType.FILE_ONLY);

        assignmentService.updateAssignment(1L, updateReq);

        assertThat(sampleAssignment.getTitle()).isEqualTo("Updated Title");
        assertThat(sampleAssignment.getSubmissionType()).isEqualTo(SubmissionType.FILE_ONLY);
        verify(assignmentRepository).save(sampleAssignment);
    }

    @Test
    @DisplayName("updateAssignment: throws when assignment not found")
    void updateAssignment_throwsWhenNotFound() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());

        AssignmentRequest req = new AssignmentRequest();
        req.setTitle("x");
        req.setDeadline(LocalDateTime.now().plusDays(1));
        req.setClassName("Y1A");
        req.setCourseName("Math");
        req.setTeacherId(1L);

        assertThatThrownBy(() -> assignmentService.updateAssignment(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  deleteAssignment
    // ----------------------------------------------------------------

    @Test
    @DisplayName("deleteAssignment: deletes existing assignment")
    void deleteAssignment_deletesWhenFound() {
        when(assignmentRepository.existsById(1L)).thenReturn(true);

        assignmentService.deleteAssignment(1L);

        verify(assignmentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteAssignment: throws when not found")
    void deleteAssignment_throwsWhenNotFound() {
        when(assignmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.deleteAssignment(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  assignmentStatus computed field
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAssignmentById: status is OVERDUE for past-deadline assignment")
    void getAssignmentById_statusOverdueForPastDeadline() {
        sampleAssignment.setDeadline(LocalDateTime.now().minusDays(1));
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssignment));

        AssignmentResponse response = assignmentService.getAssignmentById(1L);

        assertThat(response.getAssignmentStatus()).isEqualTo("OVERDUE");
    }

    @Test
    @DisplayName("getAssignmentById: status is ACTIVE for future-deadline assignment")
    void getAssignmentById_statusActiveForFutureDeadline() {
        sampleAssignment.setDeadline(LocalDateTime.now().plusDays(5));
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssignment));

        AssignmentResponse response = assignmentService.getAssignmentById(1L);

        assertThat(response.getAssignmentStatus()).isEqualTo("ACTIVE");
    }
}
