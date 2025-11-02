package org.example.repository;

import org.example.model.CalcRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalcRecordRepository extends JpaRepository<CalcRecord, Long> {
    List<CalcRecord> findBySubmittedByOrderByCreatedAtDesc(String username);
}
