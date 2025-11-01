package org.example.service;

import org.example.model.AppUser;
import org.example.model.Note;
import org.example.repository.NoteRepository;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository usersRepo;
    private final NoteRepository notesRepo;
    private final PasswordEncoder encoderRepo;

    public UserService(UserRepository users, NoteRepository notes, PasswordEncoder encoderRepo) {
        this.usersRepo = users;
        this.notesRepo = notes;
        this.encoderRepo = encoderRepo;
    }

    /** Atomic registration: create user and welcome note in one transaction. */
    @Transactional
    public void register(String username, String email, String rawPassword) {
        if (usersRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (usersRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        AppUser user = new AppUser(
                username,
                email,
                encoderRepo.encode(rawPassword)
        );

        AppUser saved = createUser(user);

        Note welcome = new Note(
                "Welcome",
                "Welcome to Note Service, " + saved.getUsername() + "!",
                saved
        );
        notesRepo.save(welcome);
    }

    public AppUser createUser(AppUser user) {
        return usersRepo.save(user);
    }

    // Get all users
    public List<AppUser> getAllUsers() {
        return usersRepo.findAll();
    }
}
