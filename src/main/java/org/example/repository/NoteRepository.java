package org.example.repository;

import org.example.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    Page<Note> findByOwnerId(Long ownerId, Pageable pageable);
    Optional<Note> findByIdAndOwnerId(Long id, Long ownerId);
}