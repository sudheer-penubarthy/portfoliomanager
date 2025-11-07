package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "amfi_nav", uniqueConstraints = {@UniqueConstraint(columnNames = {"scheme_code", "nav_date"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmfiNav {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_code", length = 32, nullable = false)
    private String schemeCode;

    @Column(name = "nav_date", nullable = false)
    private LocalDate navDate;

    @Column(name = "nav_value", precision = 28, scale = 8, nullable = false)
    private java.math.BigDecimal navValue;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
