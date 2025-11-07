package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "amfi_fund_house")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundHouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "name", length = 512, nullable = false, unique = true)
    private String name;


    @Column(name = "last_nav_date")
    private LocalDate lastNavDate;


    @Column(name = "created_at")
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
