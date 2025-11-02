package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "calc_record")
@NoArgsConstructor // JPA needs a no-args constructor
public class CalcRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String expr;

    @Column(nullable = false)
    private Integer result;

    @Column(nullable = false, length = 100)
    private String submittedBy;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore        //在序列化时忽略掉  解决history失败的问题
    private AppUser owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CalcRecord(String expr, Integer result, String submittedBy, AppUser owner) {
        this.expr = expr;
        this.result = result;
        this.submittedBy = submittedBy;
        this.owner  = owner;
        this.createdAt = Instant.now();
    }
}
