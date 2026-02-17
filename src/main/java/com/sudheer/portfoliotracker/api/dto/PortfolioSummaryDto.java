package com.sudheer.portfoliotracker.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSummaryDto {
    private Long userId;
    private String email;
    private String name;

    private BigDecimal totalInvestedAmount;
    private BigDecimal currentPortfolioValue;
    private BigDecimal totalGain;
    private Double gainPercentage;
    private Double xirr;

    private Integer totalFunds;
    private Integer totalHoldings;
}

