package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "amfi_scheme", uniqueConstraints = {@UniqueConstraint(columnNames = {"scheme_code"})})
public class AmfiScheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_code", nullable = false, length = 32)
    private String schemeCode;

    @Column(name = "isin_dividend", length = 64)
    private String isinDividend;

    @Column(name = "isin_growth", length = 64)
    private String isinGrowth;

    @Column(name = "scheme_name", length = 1024, nullable = false)
    private String schemeName;

    @Column(name = "fund_house", length = 255)
    private String fundHouse;

    @Column(name = "instrument_type", length = 255)
    private String instrumentType;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "replaced_by_scheme_code", length = 32)
    private String replacedBySchemeCode;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
