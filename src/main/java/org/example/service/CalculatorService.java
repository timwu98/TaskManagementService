package org.example.service;

import org.example.model.AppUser;
import org.example.model.CalcRecord;
import org.example.repository.CalcRecordRepository;
import org.example.repository.UserRepository;
import org.example.util.ExprCalculate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CalculatorService {
    private final CalcRecordRepository calcRepo;
    private final UserRepository usersRepo;
    private final ExprCalculate eval = new ExprCalculate();

    public CalculatorService(CalcRecordRepository calcRepo, UserRepository usersRepo) {
        this.calcRepo = calcRepo;
        this.usersRepo = usersRepo;
    }

    @Transactional
    public CalcRecord create(String username, String expr) {
        if (expr == null || expr.isBlank())
            throw new IllegalArgumentException("expr is required");

        int value = eval.calculate(expr);

        AppUser owner = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var record = new CalcRecord(expr, value, owner.getUsername(), owner);
        return calcRepo.save(record);
    }

    public List<CalcRecord> history(String username) {
        return calcRepo.findBySubmittedByOrderByCreatedAtDesc(username);
    }
}
