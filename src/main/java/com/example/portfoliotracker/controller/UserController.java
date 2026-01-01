package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.entity.PortfolioUser;
import com.example.portfoliotracker.enums.Status;
import com.example.portfoliotracker.exception.ResourceNotFoundException;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.PortfolioUserRepository;
import com.example.portfoliotracker.service.TransactionIngestService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final TransactionIngestService ingestService;
    private final AmfiImportRepository importRepository;
    private final PortfolioUserRepository userRepository;

    public UserController(TransactionIngestService ingestService,
                         AmfiImportRepository importRepository,
                         PortfolioUserRepository userRepository) {
        this.userRepository = userRepository;
        this.ingestService = ingestService;
        this.importRepository = importRepository;
    }

    /**
     * Upload and ingest transaction CSV files for a user
     *
     * @param email            the user's email address (must be valid and exist in system)
     * @param rtaName          the RTA (Registrar Transfer Agent) name
     * @param file             the CSV file containing transactions or valuations
     * @param isValuationFile  whether the file contains valuation data
     * @param importId         optional existing import ID to update
     * @return 202 Accepted with import ID for tracking progress
     * @throws ResourceNotFoundException if user with given email doesn't exist
     * @throws IllegalArgumentException  if file is empty or invalid
     */
    @PostMapping(value = "/upload-transactions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadTransactions(
            @RequestParam @NotBlank(message = "Email cannot be blank") @Email(message = "Email must be valid") String email,
            @RequestParam @NotBlank(message = "RTA name cannot be blank") String rtaName,
            @RequestPart @NotBlank(message = "File is required") MultipartFile file,
            @RequestParam boolean isValuationFile,
            @RequestParam(required = false) Long importId) {

        log.info("Starting transaction ingestion for user: {}, RTA: {}", email, rtaName);

        // Validate file is not empty
        if (file.isEmpty()) {
            log.warn("Empty file upload attempted for user: {}", email);
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        // Validate user exists in system
        userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });

        log.debug("User validated: {}", email);

        // Create or retrieve import record
        if (importId == null) {
            var imp = com.example.portfoliotracker.entity.AmfiImport.builder()
                    .fileName(file.getOriginalFilename())
                    .sourceUrl(rtaName)
                    .status(Status.PROCESSING)
                    // ✅ REMOVED manual createdAt - @CreationTimestamp handles it
                    .build();
            imp = importRepository.save(imp);
            importId = imp.getId();
            log.debug("Created new import record with ID: {}", importId);
        } else {
            log.debug("Using existing import record with ID: {}", importId);
        }

        // Process the ingestion
        log.info("Processing transactions for user: {}, RTA: {}, ImportId: {}", email, rtaName, importId);
        try {
            ingestService.ingestCsvForUser(email, rtaName, file.getInputStream(), importId, isValuationFile);
        } catch (IOException ex) {
            log.error("IO error reading uploaded file for user: {}", email, ex);
            throw new IllegalArgumentException("Failed to read uploaded file: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error during ingestion for user: {}, RTA: {}", email, rtaName, ex);
            throw new RuntimeException("Failed to ingest transactions: " + ex.getMessage(), ex);
        }

        log.info("Transaction ingestion completed successfully for user: {}, ImportId: {}", email, importId);
        return ResponseEntity.accepted().body(Map.of(
                "importId", importId,
                "status", "PROCESSING",
                "message", "File uploaded and queued for processing"
        ));
    }

    /**
     * Get portfolio snapshot for a user
     *
     * @param email the user's email address
     * @return 200 OK with snapshot data, or 404 Not Found if user doesn't exist
     */
    @GetMapping("/{email}/snapshot")
    public ResponseEntity<Map<String, Object>> snapshot(
            @PathVariable @NotBlank(message = "Email cannot be blank") @Email(message = "Email must be valid") String email) {

        try {
            log.debug("Fetching snapshot for user: {}", email);

            // Validate user exists
            userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("User not found for snapshot request: {}", email);
                        return new ResourceNotFoundException("User", "email", email);
                    });

            log.debug("User validated, retrieving snapshot for: {}", email);

            // Get snapshot data
            Map<String, Object> snap = ingestService.getUserSnapshotByEmail(email);

            if (snap.isEmpty()) {
                log.debug("No snapshot data available for user: {}", email);
                return ResponseEntity.ok(snap);  // Return empty map instead of 404
            }

            log.debug("Snapshot retrieved successfully for user: {}", email);
            return ResponseEntity.ok(snap);

        } catch (ResourceNotFoundException ex) {
            log.warn("User not found for snapshot: {}", email);
            throw ex;  // Let global exception handler manage it
        }
    }

    /**
     * Get all users with pagination support
     *
     * @param page the page number (0-indexed, default 0)
     * @param size the page size (default 20)
     * @return 200 OK with paginated list of users
     */
    @GetMapping
    public ResponseEntity<Page<PortfolioUser>> listAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            log.debug("Fetching all users - page: {}, size: {}", page, size);

            // Validate pagination parameters
            if (page < 0) {
                throw new IllegalArgumentException("Page number cannot be negative");
            }
            if (size <= 0 || size > 500) {
                throw new IllegalArgumentException("Page size must be between 1 and 500");
            }

            Page<PortfolioUser> users = userRepository.findAll(PageRequest.of(page, size));
            log.debug("Retrieved {} users from page {}", users.getNumberOfElements(), page);

            return ResponseEntity.ok(users);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid pagination parameters: {}", ex.getMessage());
            throw ex;  // Let global exception handler manage it
        }
    }
}
