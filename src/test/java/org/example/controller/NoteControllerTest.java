package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.AppUser;
import org.example.model.Note;
import org.example.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pure unit tests for NoteController using MockMvc standaloneSetup (no Spring context).
 * We provide a custom HandlerMethodArgumentResolver to satisfy @AuthenticationPrincipal UserDetails.
 * All test names and comments are in English.
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class NoteControllerStandaloneTest {

    @Mock
    private NoteService noteService;

    private MockMvc mvc;
    private ObjectMapper objectMapper;

    // --- Custom @AuthenticationPrincipal resolver (reads username from X-Test-User header) ---
    private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && UserDetails.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            String username = webRequest.getHeader("X-Test-User");
            if (username == null || username.isBlank()) {
                username = "emma"; // default for tests
            }
            return User.withUsername(username).password("N/A").roles("USER").build();
        }
    }

    @BeforeEach
    void setup() {
        openMocks(this);
        objectMapper = new ObjectMapper();
        NoteController controller = new NoteController(noteService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .build();
    }

    // --- Helpers ---
    private AppUser owner(String username) {
        AppUser u = new AppUser();
        try { AppUser.class.getMethod("setId", Long.class).invoke(u, 1L); } catch (Exception ignore) {}
        try { AppUser.class.getMethod("setUsername", String.class).invoke(u, username); } catch (Exception ignore) {}
        return u;
    }

    private Note note(Long id, String title, String content, String username) {
        Note n = new Note(title, content, owner(username));
        try { Note.class.getMethod("setId", Long.class).invoke(n, id); } catch (Exception ignore) {}
        return n;
    }

    // --- GET /api/notes (default paging) ---
    @Test
    @DisplayName("GET /api/notes should return page with default paging (page=0,size=10)")
    void list_shouldReturnPagedNotes_withDefaultPaging() throws Exception {
        PageRequest expected = PageRequest.of(0, 10);
        Page<Note> page = new PageImpl<>(
                List.of(note(10L, "A", "a", "emma")),
                expected,
                1
        );

        given(noteService.listMyNotes(eq("emma"), eq(expected))).willReturn(page);

        mvc.perform(get("/api/notes").header("X-Test-User", "emma"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("A")))
                .andExpect(jsonPath("$.content[0].content", is("a")))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.number", is(0)));

        verify(noteService).listMyNotes(eq("emma"), eq(expected));
    }

    // --- GET /api/notes (custom paging) ---
    @Test
    @DisplayName("GET /api/notes should honor page and size query params")
    void list_shouldUseProvidedPaging() throws Exception {
        PageRequest expected = PageRequest.of(2, 5);
        Page<Note> page = new PageImpl<>(
                List.of(note(11L, "B", "b", "emma")),
                expected,
                13
        );
        given(noteService.listMyNotes(eq("emma"), eq(expected))).willReturn(page);

        mvc.perform(get("/api/notes")
                        .param("page", "2")
                        .param("size", "5")
                        .header("X-Test-User", "emma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", is("B")))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(2)));

        verify(noteService).listMyNotes(eq("emma"), eq(expected));
    }

    // --- POST /api/notes ---
    @Test
    @DisplayName("POST /api/notes should create and return the note")
    void create_shouldCreateNote() throws Exception {
        var body = new NoteController.UpsertNote("Trip", "Pack");
        var created = note(42L, "Trip", "Pack", "emma");
        given(noteService.create(eq("emma"), eq("Trip"), eq("Pack"))).willReturn(created);

        mvc.perform(post("/api/notes")
                        .header("X-Test-User", "emma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is("Trip")))
                .andExpect(jsonPath("$.content", is("Pack")));

        verify(noteService).create(eq("emma"), eq("Trip"), eq("Pack"));
    }

    // --- GET /api/notes/{id} ---
    @Test
    @DisplayName("GET /api/notes/{id} should return the owned note")
    void one_shouldReturnNote() throws Exception {
        var n = note(7L, "Title", "Body", "emma");
        given(noteService.getOne(eq("emma"), eq(7L))).willReturn(n);

        mvc.perform(get("/api/notes/7").header("X-Test-User", "emma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Title")))
                .andExpect(jsonPath("$.content", is("Body")));

        verify(noteService).getOne(eq("emma"), eq(7L));
    }

    @Test
    @DisplayName("GET /api/notes/{id} should return 404 when service throws ResponseStatusException(404)")
    void one_shouldPropagate404() throws Exception {
        given(noteService.getOne(eq("emma"), eq(999L)))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Note not found"));

        mvc.perform(get("/api/notes/999").header("X-Test-User", "emma"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/notes/{id} ---
    @Test
    @DisplayName("PUT /api/notes/{id} should update and return the note")
    void update_shouldUpdateNote() throws Exception {
        var body = new NoteController.UpsertNote("New", "NewC");
        var updated = note(7L, "New", "NewC", "emma");
        given(noteService.update(eq("emma"), eq(7L), eq("New"), eq("NewC"))).willReturn(updated);

        mvc.perform(put("/api/notes/7")
                        .header("X-Test-User", "emma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New")))
                .andExpect(jsonPath("$.content", is("NewC")));

        verify(noteService).update(eq("emma"), eq(7L), eq("New"), eq("NewC"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} should return 404 when service throws not found")
    void update_shouldPropagate404() throws Exception {
        var body = new NoteController.UpsertNote("X", "Y");
        given(noteService.update(eq("emma"), eq(77L), anyString(), anyString()))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Note not found"));

        mvc.perform(put("/api/notes/77")
                        .header("X-Test-User", "emma")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/notes/{id} ---
    @Test
    @DisplayName("DELETE /api/notes/{id} should call service and return 200")
    void delete_shouldInvokeService() throws Exception {
        mvc.perform(delete("/api/notes/5").header("X-Test-User", "emma"))
                .andExpect(status().isOk());

        verify(noteService).delete(eq("emma"), eq(5L));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} should return 404 when service throws not found")
    void delete_shouldPropagate404() throws Exception {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Note not found"))
                .when(noteService).delete(eq("emma"), eq(404L));

        mvc.perform(delete("/api/notes/404").header("X-Test-User", "emma"))
                .andExpect(status().isNotFound());
    }
}
