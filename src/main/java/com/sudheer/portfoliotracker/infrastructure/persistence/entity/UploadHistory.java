package com.sudheer.portfoliotracker.infrastructure.persistence.entity;

import com.sudheer.portfoliotracker.enums.UploadProcessingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_history", indexes = {
    @Index(name = "idx_upload_user_id", columnList = "user_id"),
    @Index(name = "idx_upload_user_date", columnList = "user_id, upload_date DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UploadProcessingStatus status;

    @Column(name = "records_processed", nullable = false)
    private Integer recordsProcessed;

    @Column(name = "records_failed", nullable = false)
    private Integer recordsFailed;

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (recordsProcessed == null) {
            recordsProcessed = 0;
        }
        if (recordsFailed == null) {
            recordsFailed = 0;
        }
        if (totalRecords == null) {
            totalRecords = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate progress percentage
     */
    public Integer getProgressPercentage() {
        if (totalRecords == null || totalRecords == 0) {
            return 0;
        }
        return Math.round(((recordsProcessed != null ? recordsProcessed : 0) * 100f) / totalRecords);
    }
}

