package org.example.controller;

import org.example.model.CalcRecord;
import org.example.service.CalculatorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calc")
@Validated
public class CalculatorController {

    private final CalculatorService svc;

    public CalculatorController(CalculatorService svc) {
        this.svc = svc;
    }
    public record CalcReq(String expr) {}

    @PostMapping
    public Map<String, Object> calculate(@AuthenticationPrincipal UserDetails principal,
                                         @RequestBody CalcReq req) {
        CalcRecord saved = svc.create(principal.getUsername(), req.expr());
        return Map.of(
                "id", saved.getId(),
                "expr", saved.getExpr(),
                "result", saved.getResult(),
                "submittedBy", principal.getUsername(),
                "createdAt", saved.getCreatedAt()
        );
    }

    @GetMapping("/history")
    public List<CalcRecord> history(@AuthenticationPrincipal UserDetails principal) {
        String owner = principal.getUsername();
        return svc.history(owner);
    }
}
