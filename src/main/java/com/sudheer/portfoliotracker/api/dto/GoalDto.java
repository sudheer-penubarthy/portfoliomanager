package com.sudheer.portfoliotracker.api.dto;

import com.sudheer.portfoliotracker.enums.GoalStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal targetAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    private BigDecimal currentValue;
    private GoalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields for UI
    private BigDecimal remainingAmount;
    private Double progressPercentage;
    private Long daysRemaining;
    private String trackingStatus;
}

