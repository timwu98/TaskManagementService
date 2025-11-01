package org.example.controller;

import org.example.model.Note;
import org.example.service.NoteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    public record UpsertNote(String title, String content) {}

    @GetMapping
    public Page<Note> list(@AuthenticationPrincipal UserDetails principal,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        return noteService.listMyNotes(principal.getUsername(), PageRequest.of(page, size));
    }

    @PostMapping
    public Note create(@AuthenticationPrincipal UserDetails principal, @RequestBody UpsertNote body) {
        return noteService.create(principal.getUsername(), body.title(), body.content());
    }

    @GetMapping("/{id}")
    public Note one(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return noteService.getOne(principal.getUsername(), id);
    }

    @PutMapping("/{id}")
    public Note update(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id, @RequestBody UpsertNote body) {
        return noteService.update(principal.getUsername(), id, body.title(), body.content());
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        noteService.delete(principal.getUsername(), id);
    }
}
