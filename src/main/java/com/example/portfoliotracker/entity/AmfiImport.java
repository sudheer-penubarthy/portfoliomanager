package com.example.portfoliotracker.entity;

import com.example.portfoliotracker.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "amfi_import")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmfiImport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "file_date")
    private LocalDate fileDate;

    @Builder.Default
    @Column(name = "rows_processed")
    private Integer rowsProcessed = 0;

    @Builder.Default
    @Column(name = "rows_inserted")
    private Integer rowsInserted = 0;

    @Builder.Default
    @Column(name = "rows_skipped")
    private Integer rowsSkipped = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private Status status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_content_location", length = 1024)
    private String rawContentLocation;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at",nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        processedAt = now;
    }

}
