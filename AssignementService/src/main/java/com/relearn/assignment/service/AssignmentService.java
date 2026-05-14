package com.relearn.assignment.service;

import com.relearn.assignment.dto.AssignmentRequest;
import com.relearn.assignment.dto.AssignmentResponse;
import com.relearn.assignment.dto.AssignmentsByCourseResponse;
import com.relearn.assignment.entity.Assignment;
import com.relearn.assignment.exception.ResourceNotFoundException;
import com.relearn.assignment.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic layer for assignment management.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    // ----------------------------------------------------------------
    //  Create Assignment
    // ----------------------------------------------------------------

    @Transactional
    public AssignmentResponse createAssignment(AssignmentRequest request) {
        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .className(request.getClassName())
                .courseName(request.getCourseName())
                .teacherId(request.getTeacherId())
                .fileUrl(request.getFileUrl())
                .academicYear(request.getAcademicYear())
                .build();

        return AssignmentResponse.fromEntity(assignmentRepository.save(assignment));
    }

    // ----------------------------------------------------------------
    //  Get All Assignments
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll()
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Assignment By ID
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + id));
        return AssignmentResponse.fromEntity(assignment);
    }

    // ----------------------------------------------------------------
    //  Get Assignments By Class
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(String className) {
        return assignmentRepository.findByClassName(className)
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Assignments By Course
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByCourse(String courseName) {
        return assignmentRepository.findByCourseName(courseName)
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Assignments Grouped By Course (Student feature)
    // ----------------------------------------------------------------

    /**
     * Returns all assignments for a class, grouped by course.
     * Optionally filtered by academic year.
     */
    @Transactional(readOnly = true)
    public List<AssignmentsByCourseResponse> getAssignmentsGroupedByCourse(
            String className, String academicYear) {

        List<Assignment> assignments;
        if (academicYear != null && !academicYear.isBlank()) {
            assignments = assignmentRepository.findByClassNameAndAcademicYear(className, academicYear);
        } else {
            assignments = assignmentRepository.findByClassName(className);
        }

        Map<String, List<Assignment>> grouped = assignments.stream()
                .collect(Collectors.groupingBy(Assignment::getCourseName));

        return grouped.entrySet().stream()
                .map(entry -> new AssignmentsByCourseResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(AssignmentResponse::fromEntity)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Recent Assignments for a Class (Dashboard)
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getRecentAssignmentsByClass(String className, int limit) {
        return assignmentRepository
                .findByClassNameOrderByCreatedAtDesc(className, PageRequest.of(0, limit))
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Overdue Assignments for a Class
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getOverdueAssignmentsByClass(String className) {
        return assignmentRepository
                .findByClassNameAndDeadlineBefore(className, LocalDateTime.now())
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Active Assignments for a Class
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getActiveAssignmentsByClass(String className) {
        return assignmentRepository
                .findByClassNameAndDeadlineAfter(className, LocalDateTime.now())
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Count helpers (used by dashboard)
    // ----------------------------------------------------------------

    public long countByClassAndYear(String className, String academicYear) {
        return assignmentRepository.countByClassNameAndAcademicYear(className, academicYear);
    }

    public long countOverdueByClass(String className) {
        return assignmentRepository
                .findByClassNameAndDeadlineBefore(className, LocalDateTime.now()).size();
    }

    // ----------------------------------------------------------------
    //  Update Assignment
    // ----------------------------------------------------------------

    @Transactional
    public AssignmentResponse updateAssignment(Long id, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + id));

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDeadline(request.getDeadline());
        assignment.setClassName(request.getClassName());
        assignment.setCourseName(request.getCourseName());
        assignment.setTeacherId(request.getTeacherId());
        assignment.setFileUrl(request.getFileUrl());
        assignment.setAcademicYear(request.getAcademicYear());

        return AssignmentResponse.fromEntity(assignmentRepository.save(assignment));
    }

    // ----------------------------------------------------------------
    //  Delete Assignment
    // ----------------------------------------------------------------

    @Transactional
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Assignment not found with id: " + id);
        }
        assignmentRepository.deleteById(id);
    }
}
