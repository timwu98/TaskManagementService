package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.AppUser;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerStandaloneTest {

    @Mock
    private UserService userService;

    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    }

    @Test
    @DisplayName("POST /users: successfully creates a user")
    void createUser_ok() throws Exception {
        AppUser mockUser = new AppUser("Tim", "Tim@example.com", "hashed_pwd");
        mockUser.setId(1L);
        given(userService.createUser(any(AppUser.class))).willReturn(mockUser);

        Map<String, Object> body = new HashMap<>();
        body.put("username", "Tim");
        body.put("email", "Tim@example.com");
        body.put("password", "abc123");

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("Tim")))
                .andExpect(jsonPath("$.email", is("Tim@example.com")));

        verify(userService).createUser(any(AppUser.class));
    }

    @Test
    @DisplayName("GET /users: returns all users")
    void getAllUsers_ok() throws Exception {
        var u1 = new AppUser("Tim", "Tim@example.com", "123");
        u1.setId(1L);
        var u2 = new AppUser("Luke", "Luke@example.com", "456");
        u2.setId(2L);

        given(userService.getAllUsers()).willReturn(List.of(u1, u2));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("Tim")))
                .andExpect(jsonPath("$[1].email", is("Luke@example.com")));

        verify(userService).getAllUsers();
    }
}
