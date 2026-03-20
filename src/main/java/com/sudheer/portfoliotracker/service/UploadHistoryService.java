package com.sudheer.portfoliotracker.service;

import com.sudheer.portfoliotracker.api.dto.UploadHistoryDto;
import com.sudheer.portfoliotracker.api.dto.UploadStatusDto;
import com.sudheer.portfoliotracker.api.dto.UploadTimelineEventDto;
import com.sudheer.portfoliotracker.exception.ResourceNotFoundException;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UploadHistory;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UploadTimelineEvent;
import com.sudheer.portfoliotracker.repository.UploadHistoryRepository;
import com.sudheer.portfoliotracker.repository.UploadTimelineEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing upload history and processing status
 * Handles retrieval and management of user file uploads and their processing timeline
 */
@Slf4j
@Service
public class UploadHistoryService {

    private final UploadHistoryRepository uploadHistoryRepository;
    private final UploadTimelineEventRepository uploadTimelineEventRepository;

    public UploadHistoryService(UploadHistoryRepository uploadHistoryRepository,
                               UploadTimelineEventRepository uploadTimelineEventRepository) {
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.uploadTimelineEventRepository = uploadTimelineEventRepository;
    }

    /**
     * Get upload history for a specific user
     * Records are returned sorted by upload date in descending order (newest first)
     *
     * @param userId the ID of the user
     * @return list of upload history DTOs for the user
     */
    @Transactional(readOnly = true)
    public List<UploadHistoryDto> getUploadHistoryForUser(Long userId) {
        log.debug("Fetching upload history for user: {}", userId);

        List<UploadHistory> uploadHistories = uploadHistoryRepository.findByUserIdOrderByUploadDateDesc(userId);

        log.debug("Found {} upload history records for user: {}", uploadHistories.size(), userId);

        return uploadHistories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get current processing status for a specific upload
     *
     * @param uploadId the ID of the upload
     * @return the current status DTO
     * @throws ResourceNotFoundException if upload not found
     */
    @Transactional(readOnly = true)
    public UploadStatusDto getUploadStatus(Long uploadId) {
        log.debug("Fetching upload status for upload ID: {}", uploadId);

        UploadHistory uploadHistory = uploadHistoryRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload", "id", uploadId.toString()));

        return UploadStatusDto.builder()
                .id(uploadHistory.getId())
                .status(uploadHistory.getStatus())
                .recordsProcessed(uploadHistory.getRecordsProcessed())
                .recordsFailed(uploadHistory.getRecordsFailed())
                .totalRecords(uploadHistory.getTotalRecords())
                .errorMessage(uploadHistory.getErrorMessage())
                .build();
    }

    /**
     * Get timeline of processing events for an upload
     *
     * @param uploadId the ID of the upload
     * @return list of timeline events in chronological order (oldest first)
     * @throws ResourceNotFoundException if upload not found
     */
    @Transactional(readOnly = true)
    public List<UploadTimelineEventDto> getUploadTimeline(Long uploadId) {
        log.debug("Fetching timeline for upload ID: {}", uploadId);

        // Verify upload exists
        uploadHistoryRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload", "id", uploadId.toString()));

        // Get timeline events in chronological order (oldest first)
        List<UploadTimelineEvent> timelineEvents = uploadTimelineEventRepository.findByUploadIdOrderByTimestampAsc(uploadId);

        log.debug("Found {} timeline events for upload ID: {}", timelineEvents.size(), uploadId);

        return timelineEvents.stream()
                .map(this::convertToTimelineDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert UploadHistory entity to DTO
     *
     * @param uploadHistory the entity to convert
     * @return the DTO representation
     */
    private UploadHistoryDto convertToDto(UploadHistory uploadHistory) {
        return UploadHistoryDto.builder()
                .id(uploadHistory.getId())
                .fileName(uploadHistory.getFileName())
                .uploadDate(uploadHistory.getUploadDate())
                .status(uploadHistory.getStatus())
                .recordsProcessed(uploadHistory.getRecordsProcessed())
                .recordsFailed(uploadHistory.getRecordsFailed())
                .totalRecords(uploadHistory.getTotalRecords())
                .startedAt(uploadHistory.getStartedAt())
                .completedAt(uploadHistory.getCompletedAt())
                .errorMessage(uploadHistory.getErrorMessage())
                .build();
    }

    /**
     * Convert UploadTimelineEvent entity to DTO
     *
     * @param timelineEvent the entity to convert
     * @return the DTO representation
     */
    private UploadTimelineEventDto convertToTimelineDto(UploadTimelineEvent timelineEvent) {
        return UploadTimelineEventDto.builder()
                .timestamp(timelineEvent.getTimestamp())
                .stepName(timelineEvent.getStepName())
                .status(timelineEvent.getStatus())
                .message(timelineEvent.getMessage())
                .build();
    }
}

