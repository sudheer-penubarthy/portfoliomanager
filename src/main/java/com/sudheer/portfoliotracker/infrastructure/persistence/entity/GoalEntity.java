package com.sudheer.portfoliotracker.infrastructure.persistence.entity;

import com.sudheer.portfoliotracker.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "goal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String name;

    private String description;

    @Column(nullable = false, precision = 28, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(precision = 28, scale = 2)
    private BigDecimal currentValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private GoalStatus status;

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
        if (status == null) {
            status = GoalStatus.ACTIVE;
        }
        if (currentValue == null) {
            currentValue = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

