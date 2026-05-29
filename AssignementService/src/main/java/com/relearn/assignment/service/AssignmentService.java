package com.relearn.assignment.service;

import com.relearn.assignment.client.UserCountClient;
import com.relearn.assignment.dto.*;
import com.relearn.assignment.entity.Assignment;
import com.relearn.assignment.enums.SubmissionType;
import com.relearn.assignment.exception.ResourceNotFoundException;
import com.relearn.assignment.repository.AssignmentRepository;
import com.relearn.assignment.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic layer for assignment management.
 * Handles both student-facing and teacher-facing operations.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserCountClient userCountClient;

    // ================================================================
    //  TEACHER FEATURES
    // ================================================================

    /**
     * Returns the teacher dashboard data.
     * Aggregates: class cards, stats, recent assignments.
     */
    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(Long teacherId, String jwtToken) {
        LocalDateTime now = LocalDateTime.now();

        long totalAssignmentsGiven = assignmentRepository.countByTeacherId(teacherId);
        long totalPendingReviews   = submissionRepository.countPendingReviewByTeacherId(teacherId);

        List<Object[]> classCourses       = assignmentRepository.findDistinctClassCourseByTeacherId(teacherId);
        long totalClassAssignments        = classCourses.size();

        List<TeacherClassCardResponse> classCards = classCourses.stream()
                .map(row -> {
                    String className  = (String) row[0];
                    String courseName = (String) row[1];

                    long totalAssignments = assignmentRepository
                            .countByTeacherIdAndClassNameAndCourseName(teacherId, className, courseName);
                    long pendingReviews = submissionRepository
                            .countPendingReviewByTeacherAndClassAndCourse(teacherId, className, courseName);
                    long activeAssignments = assignmentRepository
                            .countByTeacherIdAndClassNameAndCourseNameAndDeadlineAfter(
                                    teacherId, className, courseName, now);
                    long overdueAssignments = assignmentRepository
                            .countByTeacherIdAndClassNameAndCourseNameAndDeadlineBefore(
                                    teacherId, className, courseName, now);

                    // Fetch student count from Auth Service (graceful degradation: returns 0 on failure)
                    long studentCount = userCountClient.getStudentCountForClass(className, jwtToken);

                    return TeacherClassCardResponse.builder()
                            .className(className)
                            .courseName(courseName)
                            .totalAssignments(totalAssignments)
                            .totalPendingReviews(pendingReviews)
                            .activeAssignments(activeAssignments)
                            .overdueAssignments(overdueAssignments)
                            .studentCount(studentCount)
                            .build();
                })
                .collect(Collectors.toList());

        List<AssignmentWithStatsResponse> recentAssignments =
                assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 5))
                        .stream()
                        .map(this::enrichWithStats)
                        .collect(Collectors.toList());

        return TeacherDashboardResponse.builder()
                .totalClassAssignments(totalClassAssignments)
                .totalAssignmentsGiven(totalAssignmentsGiven)
                .totalPendingReviews(totalPendingReviews)
                .classCards(classCards)
                .recentAssignments(recentAssignments)
                .build();
    }

    /**
     * Returns all assignments by a teacher, ordered:
     * 1. ACTIVE (deadline in future)
     * 2. OVERDUE (deadline in past)
     * Each includes submission stats.
     */
    @Transactional(readOnly = true)
    public List<AssignmentWithStatsResponse> getTeacherAssignments(Long teacherId) {
        LocalDateTime now = LocalDateTime.now();

        // Active first, then overdue
        List<Assignment> active = assignmentRepository
                .findByTeacherIdAndDeadlineAfter(teacherId, now);
        List<Assignment> overdue = assignmentRepository
                .findByTeacherIdAndDeadlineBefore(teacherId, now);

        List<Assignment> ordered = new ArrayList<>();
        ordered.addAll(active);
        ordered.addAll(overdue);

        return ordered.stream()
                .map(this::enrichWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Returns all assignments for a specific class+course taught by this teacher.
     */
    @Transactional(readOnly = true)
    public List<AssignmentWithStatsResponse> getTeacherAssignmentsByClassAndCourse(
            Long teacherId, String className, String courseName) {
        return assignmentRepository
                .findByTeacherIdAndClassNameAndCourseName(teacherId, className, courseName)
                .stream()
                .map(this::enrichWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Returns submission statistics for a single assignment.
     * Used for the assignment submissions page header.
     *
     * @param totalExpected total students in the class (passed from caller)
     */
    @Transactional(readOnly = true)
    public SubmissionStatsResponse getAssignmentSubmissionStats(Long assignmentId, long totalExpected) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + assignmentId));

        long totalSubmitted = submissionRepository.countByAssignmentId(assignmentId);
        long totalGraded    = submissionRepository.countByAssignmentIdAndStatus(
                assignmentId, com.relearn.assignment.enums.SubmissionStatus.GRADED);
        long totalLate      = submissionRepository.countByAssignmentIdAndStatus(
                assignmentId, com.relearn.assignment.enums.SubmissionStatus.LATE);
        long totalPending   = submissionRepository.countPendingReviewByAssignmentId(assignmentId);
        long totalMissing   = Math.max(0, totalExpected - totalSubmitted);

        return SubmissionStatsResponse.builder()
                .assignmentId(assignmentId)
                .assignmentTitle(assignment.getTitle())
                .className(assignment.getClassName())
                .courseName(assignment.getCourseName())
                .totalExpected(totalExpected)
                .totalSubmitted(totalSubmitted)
                .totalGraded(totalGraded)
                .totalPendingReview(totalPending)
                .totalLate(totalLate)
                .totalMissing(totalMissing)
                .build();
    }

    // ================================================================
    //  SHARED CRUD
    // ================================================================

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
                .submissionType(
                    request.getSubmissionType() != null
                        ? request.getSubmissionType()
                        : SubmissionType.BOTH
                )
                .build();

        return AssignmentResponse.fromEntity(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll()
                .stream()
                .map(AssignmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        return AssignmentResponse.fromEntity(
                assignmentRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Assignment not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(String className) {
        return assignmentRepository.findByClassName(className)
                .stream().map(AssignmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByCourse(String courseName) {
        return assignmentRepository.findByCourseName(courseName)
                .stream().map(AssignmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentsByCourseResponse> getAssignmentsGroupedByCourse(
            String className, String academicYear) {
        List<Assignment> assignments = (academicYear != null && !academicYear.isBlank())
                ? assignmentRepository.findByClassNameAndAcademicYear(className, academicYear)
                : assignmentRepository.findByClassName(className);

        return assignments.stream()
                .collect(Collectors.groupingBy(Assignment::getCourseName))
                .entrySet().stream()
                .map(e -> new AssignmentsByCourseResponse(
                        e.getKey(), e.getValue().size(),
                        e.getValue().stream().map(AssignmentResponse::fromEntity)
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getRecentAssignmentsByClass(String className, int limit) {
        return assignmentRepository
                .findByClassNameOrderByCreatedAtDesc(className, PageRequest.of(0, limit))
                .stream().map(AssignmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getOverdueAssignmentsByClass(String className) {
        return assignmentRepository
                .findByClassNameAndDeadlineBefore(className, LocalDateTime.now())
                .stream().map(AssignmentResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getActiveAssignmentsByClass(String className) {
        return assignmentRepository
                .findByClassNameAndDeadlineAfter(className, LocalDateTime.now())
                .stream().map(AssignmentResponse::fromEntity).collect(Collectors.toList());
    }

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
        if (request.getSubmissionType() != null) {
            assignment.setSubmissionType(request.getSubmissionType());
        }

        return AssignmentResponse.fromEntity(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Assignment not found with id: " + id);
        }
        assignmentRepository.deleteById(id);
    }

    // ================================================================
    //  Count helpers
    // ================================================================

    public long countByClassAndYear(String className, String academicYear) {
        return assignmentRepository.countByClassNameAndAcademicYear(className, academicYear);
    }

    // ================================================================
    //  Internal helpers
    // ================================================================

    /**
     * Enriches an Assignment entity with its submission statistics.
     */
    private AssignmentWithStatsResponse enrichWithStats(Assignment assignment) {
        AssignmentWithStatsResponse r = AssignmentWithStatsResponse.fromEntity(assignment);
        r.setTotalSubmitted(submissionRepository.countByAssignmentId(assignment.getId()));
        r.setTotalGraded(submissionRepository.countByAssignmentIdAndStatus(
                assignment.getId(), com.relearn.assignment.enums.SubmissionStatus.GRADED));
        r.setTotalLate(submissionRepository.countByAssignmentIdAndStatus(
                assignment.getId(), com.relearn.assignment.enums.SubmissionStatus.LATE));
        r.setTotalPendingReview(
                submissionRepository.countPendingReviewByAssignmentId(assignment.getId()));
        return r;
    }
}
