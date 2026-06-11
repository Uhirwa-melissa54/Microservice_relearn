package com.relearn.notes.service;

import com.relearn.notes.dto.NoteRequest;
import com.relearn.notes.dto.NoteResponse;
import com.relearn.notes.dto.NotesByCourseResponse;
import com.relearn.notes.entity.Note;
import com.relearn.notes.exception.ResourceNotFoundException;
import com.relearn.notes.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteService unit tests")
class NoteServiceTest {

    @Mock NoteRepository noteRepository;
    @InjectMocks NoteService noteService;

    private Note note;

    @BeforeEach
    void setUp() {
        note = Note.builder()
                .id(1L)
                .title("Chapter 1 - Calculus")
                .description("Introduction to limits")
                .fileUrl("calc-ch1.pdf")
                .className("Y1A")
                .courseName("Mathematics")
                .academicYear("2024-2025")
                .teacherId(10L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ----------------------------------------------------------------
    //  createNote
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createNote: saves note and returns response")
    void createNote_savesAndReturnsResponse() {
        NoteRequest req = buildRequest("Chapter 1 - Calculus", "Y1A", "Mathematics");
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        NoteResponse response = noteService.createNote(req);

        assertThat(response.getTitle()).isEqualTo("Chapter 1 - Calculus");
        assertThat(response.getClassName()).isEqualTo("Y1A");
        assertThat(response.getCourseName()).isEqualTo("Mathematics");
        verify(noteRepository).save(any(Note.class));
    }

    // ----------------------------------------------------------------
    //  getNoteById
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getNoteById: returns note when found")
    void getNoteById_returnsNote() {
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        NoteResponse response = noteService.getNoteById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Chapter 1 - Calculus");
    }

    @Test
    @DisplayName("getNoteById: throws when note not found")
    void getNoteById_throwsWhenNotFound() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ----------------------------------------------------------------
    //  getAllNotes
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAllNotes: returns all notes")
    void getAllNotes_returnsAll() {
        when(noteRepository.findAll()).thenReturn(List.of(note));

        List<NoteResponse> result = noteService.getAllNotes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Chapter 1 - Calculus");
    }

    // ----------------------------------------------------------------
    //  getNotesByClass
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getNotesByClass: filters by class name")
    void getNotesByClass_filtersByClassName() {
        when(noteRepository.findByClassName("Y1A")).thenReturn(List.of(note));

        List<NoteResponse> result = noteService.getNotesByClass("Y1A");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassName()).isEqualTo("Y1A");
    }

    @Test
    @DisplayName("getNotesByClass: returns empty list when no notes")
    void getNotesByClass_returnsEmptyList() {
        when(noteRepository.findByClassName("Y3Z")).thenReturn(List.of());

        List<NoteResponse> result = noteService.getNotesByClass("Y3Z");

        assertThat(result).isEmpty();
    }

    // ----------------------------------------------------------------
    //  getNotesGroupedByCourse
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getNotesGroupedByCourse: groups by course correctly")
    void getNotesGroupedByCourse_groupsByCourse() {
        Note note2 = Note.builder()
                .id(2L).title("Newton's Laws").className("Y1A")
                .courseName("Physics").academicYear("2024-2025")
                .teacherId(10L).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(noteRepository.findByClassName("Y1A")).thenReturn(List.of(note, note2));

        List<NotesByCourseResponse> result = noteService.getNotesGroupedByCourse("Y1A", null);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(NotesByCourseResponse::getCourseName))
                .containsExactlyInAnyOrder("Mathematics", "Physics");
    }

    // ----------------------------------------------------------------
    //  updateNote
    // ----------------------------------------------------------------

    @Test
    @DisplayName("updateNote: updates all fields")
    void updateNote_updatesFields() {
        NoteRequest updateReq = buildRequest("Chapter 1 Updated", "Y1A", "Mathematics");
        updateReq.setDescription("Updated description");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse response = noteService.updateNote(1L, updateReq);

        assertThat(response.getTitle()).isEqualTo("Chapter 1 Updated");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    @DisplayName("updateNote: throws when note not found")
    void updateNote_throwsWhenNotFound() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(99L, buildRequest("X", "Y", "Z")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  deleteNote
    // ----------------------------------------------------------------

    @Test
    @DisplayName("deleteNote: deletes note")
    void deleteNote_deletesSuccessfully() {
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        noteService.deleteNote(1L);

        verify(noteRepository).delete(note);
    }

    @Test
    @DisplayName("deleteNote: throws when note not found")
    void deleteNote_throwsWhenNotFound() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  getNotesByTeacher
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getNotesByTeacher: returns teacher notes newest first")
    void getNotesByTeacher_returnsTeacherNotes() {
        when(noteRepository.findByTeacherIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(note));

        List<NoteResponse> result = noteService.getNotesByTeacher(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTeacherId()).isEqualTo(10L);
    }

    // ----------------------------------------------------------------
    //  getRecentNotesByClass
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getRecentNotesByClass: limits results correctly")
    void getRecentNotesByClass_limitsResults() {
        when(noteRepository.findByClassNameOrderByCreatedAtDesc(eq("Y1A"), any(Pageable.class)))
                .thenReturn(List.of(note));

        List<NoteResponse> result = noteService.getRecentNotesByClass("Y1A", 5);

        assertThat(result).hasSize(1);
    }

    // ----------------------------------------------------------------
    //  Helper
    // ----------------------------------------------------------------

    private NoteRequest buildRequest(String title, String className, String courseName) {
        NoteRequest req = new NoteRequest();
        req.setTitle(title);
        req.setDescription("Some description");
        req.setFileUrl("file.pdf");
        req.setClassName(className);
        req.setCourseName(courseName);
        req.setAcademicYear("2024-2025");
        req.setTeacherId(10L);
        return req;
    }
}
