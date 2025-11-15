package org.example.service;

import org.example.model.AppUser;
import org.example.model.Note;
import org.example.repository.NoteRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NoteService.
 * All test names and comments are in English as requested.
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NoteService noteService;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        // assume these setters exist; if you use constructor/builder, adjust accordingly
        owner.setId(1L);
        owner.setUsername("Tim");
    }

    // ---------- Helpers ----------

    private Note newNote(String title, String content, AppUser owner) {
        // Service uses Note(title, content, owner) to create; for stubbing repository we can reuse it
        return new Note(title, content, owner);
    }

    // ---------- listMyNotes ----------

    @Test
    @DisplayName("listMyNotes(): should return paged notes for the authenticated user")
    void listMyNotes_shouldReturnNotesForOwner() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").descending());
        List<Note> data = List.of(
                newNote("A", "a", owner),
                newNote("B", "b", owner)
        );
        Page<Note> page = new PageImpl<>(data, pageable, 5);

        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByOwnerId(1L, pageable)).thenReturn(page);

        // Act
        Page<Note> result = noteService.listMyNotes("Tim", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        verify(userRepository).findByUsername("Tim");
        verify(noteRepository).findByOwnerId(1L, pageable);
    }

    @Test
    @DisplayName("listMyNotes(): should throw 404 when user not found")
    void listMyNotes_shouldThrowWhenUserMissing() {
        // Arrange
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> noteService.listMyNotes("ghost", PageRequest.of(0, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(noteRepository);
    }

    // ---------- create ----------

    @Test
    @DisplayName("create(): should persist a new note bound to owner")
    void create_shouldPersistNoteWithOwner() {
        // Arrange
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        // echo back saved entity
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Note saved = noteService.create("Tim", "Trip", "Pack checklist");

        // Assert
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getTitle()).isEqualTo("Trip");
        assertThat(saved.getContent()).isEqualTo("Pack checklist");

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        Note toSave = captor.getValue();
        assertThat(toSave.getOwner()).isSameAs(owner);
    }

    @Test
    @DisplayName("create(): should throw 404 when user not found")
    void create_shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.create("ghost", "t", "c"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(noteRepository);
    }

    // ---------- getOne ----------

    @Test
    @DisplayName("getOne(): should return note when owner matches")
    void getOne_shouldReturnOwnedNote() {
        // Arrange
        Note note = newNote("T", "C", owner);
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.of(note));

        // Act
        Note result = noteService.getOne("Tim", 99L);

        // Assert
        assertThat(result).isSameAs(note);
        verify(noteRepository).findByIdAndOwnerId(99L, 1L);
    }

    @Test
    @DisplayName("getOne(): should throw 404 when user not found")
    void getOne_shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getOne("ghost", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(noteRepository);
    }

    @Test
    @DisplayName("getOne(): should throw 404 when note not found for owner")
    void getOne_shouldThrowWhenNoteMissing() {
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwnerId(123L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getOne("Tim", 123L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- update ----------

    @Test
    @DisplayName("update(): should change title/content and save when owner matches")
    void update_shouldModifyFieldsAndSave() {
        // Arrange
        Note existing = newNote("Old", "OldC", owner);
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwnerId(7L, 1L)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Note updated = noteService.update("Tim", 7L, "New", "NewC");

        // Assert
        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getContent()).isEqualTo("NewC");
        verify(noteRepository).save(existing);
    }

    @Test
    @DisplayName("update(): should throw 404 when user not found")
    void update_shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.update("ghost", 7L, "t", "c"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(noteRepository);
    }

    @Test
    @DisplayName("update(): should throw 404 when note not found for owner")
    void update_shouldThrowWhenNoteMissing() {
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwnerId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.update("Tim", 7L, "t", "c"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(noteRepository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    @DisplayName("delete(): should delete the note when owner matches")
    void delete_shouldRemoveOwnedNote() {
        // Arrange
        Note existing = newNote("T", "C", owner);
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(existing));

        // Act
        noteService.delete("Tim", 5L);

        // Assert
        verify(noteRepository).delete(existing);
    }

    @Test
    @DisplayName("delete(): should throw 404 when user not found")
    void delete_shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.delete("ghost", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(noteRepository);
    }

    @Test
    @DisplayName("delete(): should throw 404 when note not found for owner")
    void delete_shouldThrowWhenNoteMissing() {
        when(userRepository.findByUsername("Tim")).thenReturn(Optional.of(owner));
        when(noteRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.delete("Tim", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(noteRepository, never()).delete(any());
    }
}
