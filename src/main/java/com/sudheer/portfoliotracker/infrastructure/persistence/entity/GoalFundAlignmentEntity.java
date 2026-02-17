package com.sudheer.portfoliotracker.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "goal_fund_alignment", uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "scheme_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalFundAlignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "goal_id")
    private Long goalId;

    @Column(nullable = false, length = 32, name = "scheme_code")
    private String schemeCode;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
    }
}

