package com.sudheer.portfoliotracker.repository;

import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    /**
     * Find all upload history records for a specific user, ordered by upload date descending
     *
     * @param userId the user ID
     * @return list of upload history records sorted by upload date (newest first)
     */
    List<UploadHistory> findByUserIdOrderByUploadDateDesc(Long userId);
}

