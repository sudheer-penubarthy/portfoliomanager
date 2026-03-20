package com.sudheer.portfoliotracker.api.dto;

import com.sudheer.portfoliotracker.enums.UploadProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UploadHistoryDto {
    private Long id;
    private String fileName;
    private LocalDateTime uploadDate;
    private UploadProcessingStatus status;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private Integer totalRecords;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public UploadHistoryDto() {}

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
