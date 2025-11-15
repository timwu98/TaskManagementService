package org.example.service;

import org.example.model.AppUser;
import org.example.model.Note;
import org.example.repository.NoteRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("register(): creates user and welcome note when username and email are available")
    void register_ok() {
        when(userRepository.existsByUsername(eq("Tim"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("Tim@example.com"))).thenReturn(false);
        when(passwordEncoder.encode(eq("123456"))).thenReturn("encoded123");

        AppUser persisted = new AppUser("Tim", "Tim@example.com", "encoded123");
        persisted.setId(1L);
        when(userRepository.save(any(AppUser.class))).thenReturn(persisted);

        userService.register("Tim", "Tim@example.com", "123456");

        verify(userRepository).existsByUsername("Tim");
        verify(userRepository).existsByEmail("Tim@example.com");
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(argThat(u ->
                u.getUsername().equals("Tim")
                        && u.getEmail().equals("Tim@example.com")
                        && u.getPasswordHash().equals("encoded123")
        ));
        verify(noteRepository).save(argThat(n ->
                "Welcome".equals(n.getTitle())
                        && n.getContent().contains("Tim")
                        && n.getOwner() != null
                        && n.getOwner().getId().equals(1L)
        ));
    }

    @Test
    @DisplayName("register(): throws when username already exists")
    void register_duplicate_username() {
        when(userRepository.existsByUsername(eq("Tim"))).thenReturn(true);

        assertThatThrownBy(() -> userService.register("Tim", "Tim@example.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username");

        verify(userRepository).existsByUsername("Tim");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("register(): throws when email already exists")
    void register_duplicate_email() {
        when(userRepository.existsByUsername(eq("Tim"))).thenReturn(false);
        when(userRepository.existsByEmail(eq("Tim@example.com"))).thenReturn(true);

        assertThatThrownBy(() -> userService.register("Tim", "Tim@example.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");

        verify(userRepository).existsByUsername("Tim");
        verify(userRepository).existsByEmail("Tim@example.com");
        verify(userRepository, never()).save(any());
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser(): persists and returns saved user")
    void createUser_ok() {
        AppUser toSave = new AppUser("Tim", "Tim@example.com", "encoded123");
        AppUser saved = new AppUser("Tim", "Tim@example.com", "encoded123");
        saved.setId(10L);
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);

        AppUser result = userService.createUser(toSave);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsername()).isEqualTo("Tim");
        assertThat(result.getEmail()).isEqualTo("Tim@example.com");
        verify(userRepository).save(toSave);
        verifyNoInteractions(noteRepository);
    }

    @Test
    @DisplayName("getAllUsers(): returns list of users")
    void getAllUsers_ok() {
        var u1 = new AppUser("Tim", "Tim@example.com", "1"); u1.setId(1L);
        var u2 = new AppUser("Luke", "Luke@example.com", "2"); u2.setId(2L);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        var list = userService.getAllUsers();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getEmail()).isEqualTo("Tim@example.com");
        assertThat(list.get(1).getUsername()).isEqualTo("Luke");
        verify(userRepository).findAll();
        verifyNoInteractions(noteRepository);
    }
}
