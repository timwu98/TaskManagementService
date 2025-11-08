package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.CalcRecord;
import org.example.service.CalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CalculatorControllerStandaloneTest {

    @Mock
    private CalculatorService calculatorService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            if (username == null || username.isBlank()) username = "Tim";
            return User.withUsername(username).password("N/A").roles("USER").build();
        }
    }

    @BeforeEach
    void setup() {
        var controller = new CalculatorController(calculatorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .build();
    }

    @Test
    @DisplayName("POST /api/calc: evaluates an expression and returns the saved record")
    void calculate_ok() throws Exception {
        var saved = new CalcRecord("1+2", 3, "Tim", null);
        saved.setId(10L);
        saved.setCreatedAt(Instant.now());
        given(calculatorService.create(eq("Tim"), eq("1+2"))).willReturn(saved);

        Map<String, Object> body = new HashMap<>();
        body.put("expr", "1+2");

        mockMvc.perform(post("/api/calc")
                        .header("X-Test-User", "Tim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.expr", is("1+2")))
                .andExpect(jsonPath("$.result", is(3)))
                .andExpect(jsonPath("$.submittedBy", is("Tim")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        verify(calculatorService).create(eq("Tim"), eq("1+2"));
    }

    @Test
    @DisplayName("GET /api/calc/history: fetch user's calculation history")
    void history_ok() throws Exception {
        var r = new CalcRecord("2*5", 10, "Tim", null);
        r.setId(99L);
        r.setCreatedAt(Instant.now());
        given(calculatorService.history(eq("Tim"))).willReturn(List.of(r));

        mockMvc.perform(get("/api/calc/history")
                        .header("X-Test-User", "Tim"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(99)))
                .andExpect(jsonPath("$[0].expr", is("2*5")))
                .andExpect(jsonPath("$[0].result", is(10)))
                .andExpect(jsonPath("$[0].submittedBy", is("Tim")));

        verify(calculatorService).history(eq("Tim"));
    }
}
