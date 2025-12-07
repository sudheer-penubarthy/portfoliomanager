package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "user_holding", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "scheme_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHolding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long userId;
    private String schemeCode;

    @Column(precision = 28, scale = 8)
    private BigDecimal units;

    @Column(precision = 28, scale = 8)
    private BigDecimal avgCostPerUnit;

    @Column(precision = 28, scale = 8)
    private BigDecimal totalCost;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
