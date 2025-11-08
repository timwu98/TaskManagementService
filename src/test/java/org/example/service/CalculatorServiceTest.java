package org.example.service;

import org.example.model.AppUser;
import org.example.model.CalcRecord;
import org.example.repository.CalcRecordRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculatorServiceTest {

    @Mock
    private CalcRecordRepository calcRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CalculatorService calculatorService;

    private AppUser owner;

//    private AppUser user(String username) {
//        AppUser u = new AppUser();
//        u.setId(1L);
//        u.setUsername(username);
//        return u;
//    }

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setId(1L);
        owner.setUsername("Tim");
    }

    @Test
    @DisplayName("create(): should evaluate expression, bind user, persist and return")
    void create_ok() {
        when(userRepository.findByUsername(eq("Tim"))).thenReturn(Optional.of(owner));
        when(calcRecordRepository.save(any(CalcRecord.class))).thenAnswer(inv -> {
            CalcRecord r = inv.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(Instant.now());
            return r;
        });

        CalcRecord saved = calculatorService.create("Tim", "1+2*3");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExpr()).isEqualTo("1+2*3");
        assertThat(saved.getResult()).isEqualTo(7);
        assertThat(saved.getSubmittedBy()).isEqualTo("Tim");
        verify(userRepository).findByUsername("Tim");
        verify(calcRecordRepository).save(any(CalcRecord.class));
    }

    @Test
    @DisplayName("create(): should reject blank expression")
    void create_blank_expr() {
        assertThatThrownBy(() -> calculatorService.create("Tim", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculatorService.create("Tim", null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(calcRecordRepository);
    }

    @Test
    @DisplayName("create(): should throw 404 when user not found")
    void create_user_not_found() {
        when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculatorService.create("ghost", "1+1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(calcRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("history(): should return records ordered by createdAt desc for submitter")
    void history_ok() {
        when(calcRecordRepository.findBySubmittedByOrderByCreatedAtDesc(eq("Tim")))
                .thenReturn(List.of(new CalcRecord("2*5", 10, "Tim", owner)));

        List<CalcRecord> list = calculatorService.history("Tim");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getExpr()).isEqualTo("2*5");
        assertThat(list.get(0).getResult()).isEqualTo(10);
        verify(calcRecordRepository).findBySubmittedByOrderByCreatedAtDesc("Tim");
    }
}
