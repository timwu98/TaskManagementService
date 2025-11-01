package org.example.service;

import lombok.val;
import org.example.model.AppUser;
import org.example.model.Note;
import org.example.repository.NoteRepository;
import org.example.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * NoteService handles all business logic for notes.
 * Controller should never directly call repository.
 */
@Service
public class NoteService {

    private final NoteRepository notesRepo;
    private final UserRepository usersRepo;

    public NoteService(NoteRepository notes, UserRepository usersRepo) {
        this.notesRepo = notes;
        this.usersRepo = usersRepo;
    }

    /** List all notes for the authenticated user. */
    public Page<Note> listMyNotes(String username, Pageable pageable) {
        AppUser owner = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return notesRepo.findByOwnerId(owner.getId(), pageable);
    }

    /** Create a note (with ownership binding). */
    @Transactional
    public Note create(String username, String title, String content) {
        AppUser owner = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Use constructor instead of setters
        Note note = new Note(title, content, owner);
        return notesRepo.save(note);
    }

    /** Retrieve one note, ensuring ownership validation. */
    public Note getOne(String username, Long id) {
        AppUser owner = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return notesRepo.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
    }

    /** Update a note, ensuring user owns it. */
    @Transactional
    public Note update(String username, Long id, String title, String content) {
        AppUser owner = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Note note = notesRepo.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        note.setTitle(title);
        note.setContent(content);
        return notesRepo.save(note);
    }

    /** Delete a note, ensuring user owns it. */
    @Transactional
    public void delete(String username, Long id) {
        AppUser owner = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Note note = notesRepo.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        notesRepo.delete(note);
    }
}
