package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.service.ResetService;
import com.example.portfoliotracker.service.ResetService.ResetSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for resetting test data.
 *
 * WARNING: This controller is for development and testing purposes only.
 * It provides endpoints to clear user-related data from the database.
 * This controller and its endpoints should be disabled in production environments.
 *
 * @deprecated This controller is temporary and should be removed before production deployment.
 */
@Slf4j
@RestController
@RequestMapping("/api/reset")
@Deprecated(since = "0.1.0", forRemoval = true)
public class ResetController {

    private final ResetService resetService;

    public ResetController(ResetService resetService) {
        this.resetService = resetService;
    }

    /**
     * Reset all user-related data (users, transactions, holdings).
     *
     * @return A summary of deleted records
     *
     * @deprecated For testing/development only. Remove before production.
     */
    @DeleteMapping("/users")
    @Deprecated(since = "0.1.0", forRemoval = true)
    public ResponseEntity<ResetSummary> resetUserData() {
        log.warn("Reset endpoint called - clearing all user-related data");
        ResetSummary summary = resetService.resetUserData();
        return ResponseEntity.ok(summary);
    }

    /**
     * Health check endpoint for the reset controller.
     * Returns a warning message indicating this is for testing only.
     *
     * @return A warning message
     */
    @DeleteMapping("/health")
    public ResponseEntity<String> resetHealth() {
        String message = "Reset controller is active. " +
                "WARNING: This is for development/testing only. " +
                "Ensure this endpoint is disabled in production.";
        log.warn(message);
        return ResponseEntity.ok(message);
    }
}

