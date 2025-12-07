package com.example.portfoliotracker.repository;

import com.example.portfoliotracker.entity.AmfiImport;
import com.example.portfoliotracker.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AmfiImportRepository extends JpaRepository<AmfiImport, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE AmfiImport a SET a.rowsProcessed = :rowsProcessed, a.rowsInserted = :rowsInserted, a.rowsSkipped = :rowsSkipped WHERE a.id = :id")
    int updateProgress(long id, int rowsProcessed, int rowsInserted, int rowsSkipped);

    @Modifying
    @Transactional
    @Query("UPDATE AmfiImport a SET a.status = :status, a.errorMessage = :error WHERE a.id = :id")
    int updateStatus(long id, String status, String error);

    @Modifying
    @Transactional
    @Query("UPDATE AmfiImport a SET a.status = :status, a.processedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    int markCompleted(long id, Status status);

}
