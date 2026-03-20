package com.sudheer.portfoliotracker.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_timeline_event", indexes = {
    @Index(name = "idx_timeline_upload_id", columnList = "upload_id"),
    @Index(name = "idx_timeline_timestamp", columnList = "timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadTimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", nullable = false)
    private Long uploadId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "message", length = 1000)
    private String message;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

