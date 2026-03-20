package com.sudheer.portfoliotracker.api.dto;

import com.sudheer.portfoliotracker.enums.UploadProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UploadStatusDto {
    private Long id;
    private UploadProcessingStatus status;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private Integer totalRecords;
    private String errorMessage;

    public UploadStatusDto() {}

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

