package com.relearn.notes.service;

import com.relearn.notes.dto.NoteRequest;
import com.relearn.notes.dto.NoteResponse;
import com.relearn.notes.dto.NotesByCourseResponse;
import com.relearn.notes.entity.Note;
import com.relearn.notes.exception.ResourceNotFoundException;
import com.relearn.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic layer for the Notes Service.
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    // ----------------------------------------------------------------
    //  Create Note
    // ----------------------------------------------------------------

    @Transactional
    public NoteResponse createNote(NoteRequest request) {
        Note note = Note.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(request.getFileUrl())
                .className(request.getClassName())
                .courseName(request.getCourseName())
                .academicYear(request.getAcademicYear())
                .teacherId(request.getTeacherId())
                .build();

        Note savedNote = noteRepository.save(note);
        return NoteResponse.fromEntity(savedNote);
    }

    // ----------------------------------------------------------------
    //  Get All Notes
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NoteResponse> getAllNotes() {
        return noteRepository.findAll()
                .stream()
                .map(NoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Note By ID
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public NoteResponse getNoteById(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found with id: " + id));
        return NoteResponse.fromEntity(note);
    }

    // ----------------------------------------------------------------
    //  Get Notes By Class
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByClass(String className) {
        return noteRepository.findByClassName(className)
                .stream()
                .map(NoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Notes By Course
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByCourse(String courseName) {
        return noteRepository.findByCourseName(courseName)
                .stream()
                .map(NoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Notes By Academic Year
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByAcademicYear(String academicYear) {
        return noteRepository.findByAcademicYear(academicYear)
                .stream()
                .map(NoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Notes Grouped By Course (Student feature)
    // ----------------------------------------------------------------

    /**
     * Returns all notes for a class, grouped by course name.
     * This is the primary student view — they see all courses with their notes.
     *
     * @param className   the student's class (e.g. "Y1A")
     * @param academicYear optional filter by academic year
     */
    @Transactional(readOnly = true)
    public List<NotesByCourseResponse> getNotesGroupedByCourse(String className, String academicYear) {
        List<Note> notes;
        if (academicYear != null && !academicYear.isBlank()) {
            notes = noteRepository.findByClassNameAndAcademicYear(className, academicYear);
        } else {
            notes = noteRepository.findByClassName(className);
        }

        // Group notes by courseName using Java streams
        Map<String, List<Note>> grouped = notes.stream()
                .collect(Collectors.groupingBy(Note::getCourseName));

        return grouped.entrySet().stream()
                .map(entry -> new NotesByCourseResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(NoteResponse::fromEntity)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Recent Notes for a Class (Student dashboard)
    // ----------------------------------------------------------------

    /**
     * Returns the N most recently uploaded notes for a class.
     * Used to populate the "recent notes" section on the student dashboard.
     *
     * @param className the student's class
     * @param limit     how many recent notes to return (default 5)
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getRecentNotesByClass(String className, int limit) {
        return noteRepository
                .findByClassNameOrderByCreatedAtDesc(className, PageRequest.of(0, limit))
                .stream()
                .map(NoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Notes By Class and Academic Year (History)
    // ----------------------------------------------------------------

    /**
     * Returns all notes for a class in a specific academic year.
     * Used for the academic history feature.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByClassAndYear(String className, String academicYear) {
        return noteRepository.findByClassNameAndAcademicYear(className, academicYear)
                .stream()
                .map(NoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Count Notes (used by dashboard)
    // ----------------------------------------------------------------

    public long countByClassAndYear(String className, String academicYear) {
        return noteRepository.countByClassNameAndAcademicYear(className, academicYear);
    }

    // ----------------------------------------------------------------
    //  Teacher-specific methods
    // ----------------------------------------------------------------

    /**
     * Returns all notes uploaded by a teacher, newest first.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByTeacher(Long teacherId) {
        return noteRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream().map(NoteResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Returns notes for a specific class+course uploaded by this teacher.
     * Used on the teacher's class details page.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByTeacherAndClassAndCourse(
            Long teacherId, String className, String courseName) {
        return noteRepository.findByTeacherIdAndClassNameAndCourseName(
                        teacherId, className, courseName)
                .stream().map(NoteResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Returns total notes count for a teacher.
     */
    public long countNotesByTeacher(Long teacherId) {
        return noteRepository.countByTeacherId(teacherId);
    }

    /**
     * Returns notes count for a teacher per class+course.
     */
    public long countNotesByTeacherAndClassAndCourse(
            Long teacherId, String className, String courseName) {
        return noteRepository.countByTeacherIdAndClassNameAndCourseName(
                teacherId, className, courseName);
    }

    // ----------------------------------------------------------------
    //  Update Note
    // ----------------------------------------------------------------

    @Transactional
    public NoteResponse updateNote(Long id, NoteRequest request) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found with id: " + id));

        note.setTitle(request.getTitle());
        note.setDescription(request.getDescription());
        note.setFileUrl(request.getFileUrl());
        note.setClassName(request.getClassName());
        note.setCourseName(request.getCourseName());
        note.setAcademicYear(request.getAcademicYear());
        note.setTeacherId(request.getTeacherId());

        Note updatedNote = noteRepository.save(note);
        return NoteResponse.fromEntity(updatedNote);
    }

    // ----------------------------------------------------------------
    //  Delete Note
    // ----------------------------------------------------------------

    @Transactional
    public void deleteNote(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found with id: " + id));
        // Physical file is deleted by the controller/FileStorageService
        // before calling this method
        noteRepository.delete(note);
    }

    /**
     * Returns the stored filename for a note (used for file deletion on update).
     */
    @Transactional(readOnly = true)
    public String getNoteFilename(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found with id: " + id));
        return note.getFileUrl();
    }
}
