package com.sudheer.portfoliotracker.api.controller;

import com.sudheer.portfoliotracker.api.dto.UploadHistoryDto;
import com.sudheer.portfoliotracker.api.dto.UploadStatusDto;
import com.sudheer.portfoliotracker.api.dto.UploadTimelineEventDto;
import com.sudheer.portfoliotracker.service.JwtTokenService;
import com.sudheer.portfoliotracker.service.UploadHistoryService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Upload History", description = "User file upload history endpoints")
public class UploadHistoryController {

    private final UploadHistoryService uploadHistoryService;
    private final JwtTokenService jwtTokenService;

    public UploadHistoryController(UploadHistoryService uploadHistoryService, JwtTokenService jwtTokenService) {
        this.uploadHistoryService = uploadHistoryService;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Get upload history for the currently authenticated user
     *
     * @param authorizationHeader the Authorization header containing the JWT token
     * @return 200 OK with list of upload history records for the user
     */
    @GetMapping("/history")
    @Operation(
            summary = "Get user upload history",
            description = "Retrieves the upload history for the currently authenticated user. Returns uploads sorted by date (newest first)."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Upload history retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadHistoryDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid token format"
            )
    })
    public ResponseEntity<List<UploadHistoryDto>> getUploadHistory(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        log.debug("Received request to fetch upload history");

        // Extract user ID from JWT token
        Long userId = extractUserIdFromToken(authorizationHeader);
        if (userId == null) {
            log.warn("Failed to extract user ID from token");
            return ResponseEntity.badRequest().build();
        }

        log.debug("Fetching upload history for user: {}", userId);

        // Get upload history from service
        List<UploadHistoryDto> uploadHistory = uploadHistoryService.getUploadHistoryForUser(userId);

        log.debug("Successfully retrieved {} upload records for user: {}", uploadHistory.size(), userId);

        return ResponseEntity.ok(uploadHistory);
    }

    /**
     * Get current processing status for a specific upload
     *
     * @param uploadId the upload ID
     * @return 200 OK with current status
     */
    @GetMapping("/{uploadId}/status")
    @Operation(
            summary = "Get upload processing status",
            description = "Retrieves the current processing status of a specific upload, including progress percentage."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadStatusDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Upload not found"
            )
    })
    public ResponseEntity<UploadStatusDto> getUploadStatus(@PathVariable Long uploadId) {
        log.debug("Fetching upload status for upload ID: {}", uploadId);

        UploadStatusDto status = uploadHistoryService.getUploadStatus(uploadId);

        return ResponseEntity.ok(status);
    }

    /**
     * Get timeline of processing events for a specific upload
     *
     * @param uploadId the upload ID
     * @return 200 OK with timeline events
     */
    @GetMapping("/{uploadId}/timeline")
    @Operation(
            summary = "Get upload processing timeline",
            description = "Retrieves the chronological timeline of processing events for a specific upload."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Timeline retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadTimelineEventDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Upload not found"
            )
    })
    public ResponseEntity<List<UploadTimelineEventDto>> getUploadTimeline(@PathVariable Long uploadId) {
        log.debug("Fetching timeline for upload ID: {}", uploadId);

        List<UploadTimelineEventDto> timeline = uploadHistoryService.getUploadTimeline(uploadId);

        return ResponseEntity.ok(timeline);
    }
     /*
     * @param authorizationHeader the Authorization header value
     * @return the user ID from the token, or null if invalid
     */
    private Long extractUserIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            log.warn("Authorization header is missing");
            return null;
        }

        // Extract token from "Bearer <token>" format
        final String BEARER_PREFIX = "Bearer ";
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization header does not start with 'Bearer '");
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        // Extract user ID from token claims
        Claims claims = jwtTokenService.validateToken(token);
        if (claims == null) {
            log.warn("Token validation failed");
            return null;
        }

        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            log.warn("userId claim not found in token");
            return null;
        }

        return userId;
    }
}

