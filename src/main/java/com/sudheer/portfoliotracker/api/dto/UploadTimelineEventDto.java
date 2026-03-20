package com.sudheer.portfoliotracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UploadTimelineEventDto {
    private LocalDateTime timestamp;
    private String stepName;
    private String status;
    private String message;

    public UploadTimelineEventDto() {}
}

