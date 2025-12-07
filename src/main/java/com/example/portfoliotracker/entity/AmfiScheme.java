package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "amfi_scheme", uniqueConstraints = {@UniqueConstraint(columnNames = {"scheme_code"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    // legacy free-text fund house name (kept for backwards compat)
    @Column(name = "fund_house", length = 255)
    private String fundHouse;

    // normalized relation to fund house
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_house_id")
    private FundHouse fundHouseEntity;

    @Column(name = "instrument_type", length = 255)
    private String instrumentType;


    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "replaced_by_scheme_code", length = 32)
    private String replacedBySchemeCode;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    public FundHouse getFundHouseEntity() {
        return fundHouseEntity;
    }
}
