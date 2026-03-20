package com.sudheer.portfoliotracker.repository;

import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UploadTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadTimelineEventRepository extends JpaRepository<UploadTimelineEvent, Long> {
    /**
     * Find all timeline events for a specific upload, ordered by timestamp descending
     *
     * @param uploadId the upload ID
     * @return list of timeline events sorted by timestamp (newest first)
     */
    List<UploadTimelineEvent> findByUploadIdOrderByTimestampDesc(Long uploadId);

    /**
     * Find all timeline events for a specific upload, ordered by timestamp ascending
     *
     * @param uploadId the upload ID
     * @return list of timeline events sorted by timestamp (oldest first)
     */
    List<UploadTimelineEvent> findByUploadIdOrderByTimestampAsc(Long uploadId);
}

