package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "fund")
public class FundEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "amfi_code", unique = true)
    private String amfiCode;

    // getters and setters
}
