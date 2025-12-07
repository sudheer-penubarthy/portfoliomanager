package com.example.portfoliotracker.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;


@Entity
@Table(name = "external_scheme_map", uniqueConstraints = @UniqueConstraint(columnNames = {"rta_name", "external_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalSchemeMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String rtaName;
    private String externalCode;
    private String schemeCode;


    @Column(name = "created_at")
    private LocalDateTime createdAt;
}