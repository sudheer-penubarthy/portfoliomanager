package com.example.portfoliotracker.entity;

import com.example.portfoliotracker.enums.TxnType;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "user_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long userId;
    private String txnSource;
    private String sourceReference;
    private LocalDate txnDate;
    private String schemeCode;
    @Enumerated(EnumType.STRING)
    private TxnType txnType;

    @Column(precision = 28, scale = 8)
    private BigDecimal units;

    @Column(precision = 28, scale = 8)
    private BigDecimal amount;

    @Column(precision = 28, scale = 8)
    private BigDecimal pricePerUnit;

    private String remarks;
    private Long importId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
    }

}