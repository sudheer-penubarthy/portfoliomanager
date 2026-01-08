package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.entity.AmfiImport;
import com.example.portfoliotracker.entity.PortfolioUser;
import com.example.portfoliotracker.enums.Status;
import com.example.portfoliotracker.exception.ResourceNotFoundException;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.PortfolioUserRepository;
import com.example.portfoliotracker.service.TransactionIngestService;
import com.example.portfoliotracker.service.ZipHandlerService;
import com.example.portfoliotracker.util.StringUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.HashMap;
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
     * @param email           the user's email address (must be valid and exist in system)
     * @param rtaName         the RTA (Registrar Transfer Agent) name
     * @param file            the CSV file containing transactions or valuations
     * @param isValuationFile whether the file contains valuation data
     * @param importId        optional existing import ID to update
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

    /**
     * Upload multiple files (transaction and valuation) or a ZIP archive for a user.
     * Supports:
     * - Individual transaction and valuation files
     * - ZIP archive containing both files
     * <p>
     * Naming conventions:
     * - Transaction file: alphanumeric string (e.g., "ABC123.txt")
     * - Valuation file: "CurrentValuation" or "CurrentValuation.txt"
     *
     * @param email           the user's email address
     * @param rtaName         the RTA (Registrar Transfer Agent) name
     * @param transactionFile optional transaction file
     * @param valuationFile   optional valuation/snapshot file
     * @param zipFile         optional ZIP archive containing both files
     * @param importId        optional existing import ID to update
     * @return 202 Accepted with import ID for tracking progress
     * @throws ResourceNotFoundException if user with given email doesn't exist
     * @throws IllegalArgumentException  if files are invalid or no files provided
     */
    @PostMapping(value = "/upload-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam @NotBlank(message = "Email cannot be blank") @Email(message = "Email must be valid") String email,
            @RequestParam @NotBlank(message = "RTA name cannot be blank") String rtaName,
            @RequestPart(required = false) MultipartFile transactionFile,
            @RequestPart(required = false) MultipartFile valuationFile,
            @RequestPart(required = false) MultipartFile zipFile,
            @RequestParam(required = false) String zipPassword,
            @RequestParam(required = false) Long importId) {

        log.info("Starting multi-file upload for user: {}, RTA: {}", email, rtaName);

        // Check if user exists, will create later if needed
        PortfolioUser user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = user == null;

        if (isNewUser) {
            log.info("User not found with email: {}. New user will be created after extracting metadata from files", email);
        } else {
            log.debug("User validated: {}", email);
        }

        // Determine upload mode and validate files
        boolean isZipUpload = zipFile != null && !zipFile.isEmpty();
        boolean hasTransactionFile = transactionFile != null && !transactionFile.isEmpty();
        boolean hasValuationFile = valuationFile != null && !valuationFile.isEmpty();

        if (!isZipUpload && !hasTransactionFile && !hasValuationFile) {
            log.warn("No files provided for upload by user: {}", email);
            throw new IllegalArgumentException(
                    "At least one file must be provided: transaction file, valuation file, or ZIP archive");
        }

        log.info("Upload mode - ZIP: {}, Transaction: {}, Valuation: {}",
                isZipUpload, hasTransactionFile, hasValuationFile);

        try {
            if (isZipUpload) {
                return handleZipUpload(user, email, rtaName, zipFile, zipPassword, importId, isNewUser);
            } else {
                return handleIndividualFileUpload(user, email, rtaName, transactionFile, valuationFile, importId, isNewUser);
            }
        } catch (IOException ex) {
            log.error("IO error during file upload for user: {}", email, ex);
            throw new IllegalArgumentException("Failed to process uploaded files: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error during file upload for user: {}, RTA: {}", email, rtaName, ex);
            throw new RuntimeException("Failed to process files: " + ex.getMessage(), ex);
        }
    }

    /**
     * Handle ZIP file upload by extracting and processing transaction/valuation files.
     */
    private ResponseEntity<Map<String, Object>> handleZipUpload(
            PortfolioUser user,
            String email,
            String rtaName,
            MultipartFile zipFile,
            String zipPassword,
            Long importId,
            boolean isNewUser) throws Exception {

        log.info("Processing ZIP upload for user: {}", email);

        // 1. Extract ZIP (validation + naming rules enforced inside service)
        ZipHandlerService.ExtractedFiles extracted =
                (zipPassword != null && !zipPassword.isEmpty())
                        ? ingestService.extractFilesFromZip(zipFile.getInputStream(), zipPassword)
                        : ingestService.extractFilesFromZip(zipFile.getInputStream());

        // ZIP rules already enforced inside ZipHandlerService
        // 2. New user → extract metadata
        if (isNewUser) {
            Map<String, String> metadata =
                    extractUserMetadata(
                            extracted.getTransactionFile(),
                            extracted.getValuationFile(),
                            rtaName
                    );

            user = userRepository.save(
                    PortfolioUser.builder()
                            .email(email)
                            .pan(metadata.get("pan"))
                            .name(metadata.get("investorName"))
                            .build()
            );

            log.info("Created new user: {}, PAN: {}", email, metadata.get("pan"));
        }

        // 3. Create import record if needed
        if (importId == null) {
            var imp = AmfiImport.builder()
                    .fileName(zipFile.getOriginalFilename())
                    .sourceUrl(rtaName)
                    .status(Status.PROCESSING)
                    .build();

            imp = importRepository.save(imp);
            importId = imp.getId();
            log.debug("Created import record with ID: {}", importId);
        }

        long finalImportId = importId;

        // 4. Process valuation file FIRST
        if (extracted.hasValuationFile()) {
            log.info("Processing valuation file for user: {}", email);

            ingestService.ingestCsvForUser(
                    email,
                    rtaName,
                    extracted.getValuationFileBytes(),
                    finalImportId,
                    true
            );
        }

        // 5. Process transaction file
        if (extracted.hasTransactionFile()) {
            log.info("Processing transaction file for user: {}", email);

            ingestService.ingestCsvForUser(
                    email,
                    rtaName,
                    extracted.getTransactionFile(),
                    finalImportId,
                    false
            );
        }

        log.info("ZIP upload completed successfully for user: {}, ImportId: {}", email, importId);

        return ResponseEntity.accepted().body(Map.of(
                "importId", importId,
                "status", "PROCESSING",
                "uploadMode", "ZIP",
                "filesProcessed", 2,
                "message", "ZIP file uploaded and queued for processing"
        ));

    }

    /**
     * Handle individual file upload (transaction and/or valuation files).
     */
    private ResponseEntity<Map<String, Object>> handleIndividualFileUpload(
            PortfolioUser user,
            String email,
            String rtaName,
            MultipartFile transactionFile,
            MultipartFile valuationFile,
            Long importId,
            boolean isNewUser) throws Exception {

        log.info("Processing individual file upload for user: {}", email);

        // If new user, extract PAN and investor name from files before creating user
        if (isNewUser) {
            log.info("Extracting user metadata from files for new user: {}", email);
            Map<String, String> metadata = new HashMap<>();

            // Try to extract from valuation file first (likely has PAN)
            if (valuationFile != null && !valuationFile.isEmpty()) {
                metadata.putAll(extractMetadataFromFile(valuationFile, rtaName));
            }

            // Then try transaction file if metadata not complete
            if (transactionFile != null && !transactionFile.isEmpty() &&
                    (metadata.get("pan") == null || metadata.get("investorName") == null)) {
                metadata.putAll(extractMetadataFromFile(transactionFile, rtaName));
            }

            String pan = metadata.get("pan");
            String investorName = metadata.get("investorName");

            user = PortfolioUser.builder()
                    .email(email)
                    .pan(pan)
                    .name(investorName)
                    .build();
            user = userRepository.save(user);
            log.info("New user created with email: {}, PAN: {}, Name: {}", email, pan, investorName);
        }

        // Create or retrieve import record
        if (importId == null) {
            String fileName = transactionFile != null ? transactionFile.getOriginalFilename() :
                    valuationFile != null ? valuationFile.getOriginalFilename() : "upload.csv";
            var imp = com.example.portfoliotracker.entity.AmfiImport.builder()
                    .fileName(fileName)
                    .sourceUrl(rtaName)
                    .status(Status.PROCESSING)
                    .build();
            imp = importRepository.save(imp);
            importId = imp.getId();
            log.debug("Created new import record with ID: {}", importId);
        }

        long finalImportId = importId;

        // Process valuation file if provided
        if (valuationFile != null && !valuationFile.isEmpty()) {
            log.info("Processing valuation file for user: {}", email);
            try {
                ingestService.ingestCsvForUser(email, rtaName, valuationFile.getInputStream(),
                        finalImportId, true);
            } catch (Exception ex) {
                log.error("Error processing valuation file for user: {}", email, ex);
                throw ex;
            }
        }

        // Process transaction file if provided
        if (transactionFile != null && !transactionFile.isEmpty()) {
            log.info("Processing transaction file for user: {}", email);
            try {
                ingestService.ingestCsvForUser(email, rtaName, transactionFile.getInputStream(),
                        finalImportId, false);
            } catch (Exception ex) {
                log.error("Error processing transaction file for user: {}", email, ex);
                throw ex;
            }
        }

        log.info("Individual file upload completed successfully for user: {}, ImportId: {}",
                email, importId);

        int filesProcessed = (transactionFile != null && !transactionFile.isEmpty() ? 1 : 0) +
                (valuationFile != null && !valuationFile.isEmpty() ? 1 : 0);

        return ResponseEntity.accepted().body(Map.of(
                "importId", importId,
                "status", "PROCESSING",
                "uploadMode", "INDIVIDUAL",
                "filesProcessed", filesProcessed,
                "message", "File(s) uploaded and queued for processing"
        ));
    }

    /**
     * Extract user metadata (PAN and investor name) from uploaded files.
     * Supports extraction from both transaction and valuation files.
     *
     * @param extracted the extracted files object containing transaction and valuation files
     * @param rtaName   the RTA name, used for logging and processing rules
     * @return a map containing extracted metadata fields: pan, investorName
     * @throws IOException if an error occurs during metadata extraction
     */
    private Map<String, String> extractUserMetadata(
            byte[] transactionFileBytes,
            byte[] valuationFileBytes,
            String rtaName) throws IOException {

        Map<String, String> metadata = new HashMap<>();

        // Prefer valuation file if present
        if (valuationFileBytes != null) {
            metadata.putAll(
                    extractMetadataFromBytes(valuationFileBytes, rtaName));
        }

        // Fall back to transaction file if needed
        if (transactionFileBytes != null &&
                (metadata.get("pan") == null || metadata.get("investorName") == null)) {

            metadata.putAll(
                    extractMetadataFromBytes(transactionFileBytes, rtaName));
        }

        // Validate mandatory fields
        if (metadata.get("pan") == null || metadata.get("investorName") == null) {
            throw new IOException(
                    "Mandatory metadata fields missing: PAN and/or Investor Name");
        }

        return metadata;
    }

    private Map<String, String> extractMetadataFromBytes(
            byte[] data, String rtaName) throws IOException {

        Charset charset = StringUtil.detectCharset(data); // or force UTF-8

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new ByteArrayInputStream(data), charset))) {

            return processExtractedData(reader, rtaName);
        }
    }

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");



    /**
     * Extract metadata fields from a CSV file.
     * Supports extraction of PAN and investor name based on file content.
     * This method reads the first few lines to identify and extract investor metadata.
     *
     * @param file    the CSV file from which metadata needs to be extracted
     * @param rtaName the RTA name, used for logging and processing rules
     * @return a map containing extracted metadata fields: pan, investorName
     * @throws IOException if an error occurs during metadata extraction
     */
    private Map<String, String> extractMetadataFromFile(MultipartFile file, String rtaName) throws IOException {
        Map<String, String> metadata = new HashMap<>();

        // Basic validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required for metadata extraction");
        }

        String fileName = file.getOriginalFilename();
        log.info("Extracting metadata from file: {} (RTA: {})", fileName, rtaName);

        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(file.getInputStream()))) {
            processExtractedData(reader, metadata);
        }

        // If we couldn't extract metadata, use defaults
        if (metadata.get("pan") == null) {
            log.warn("PAN not found in file: {}", fileName);
            metadata.put("pan", "UNKNOWN");
        }

        if (metadata.get("investorName") == null) {
            log.warn("Investor name not found in file: {}", fileName);
            metadata.put("investorName", "Unknown Investor");
        }

        log.info("Extracted metadata - PAN: {}, Investor Name: {}", metadata.get("pan"), metadata.get("investorName"));
        return metadata;
    }

    private static void processExtractedData(BufferedReader reader, Map<String, String> metadata) throws IOException {
        String line;
        int lineNum = 0;
        StringBuilder fullContent = new StringBuilder();

        // Read first 50 lines to find PAN and investor name
        while ((line = reader.readLine()) != null && lineNum < 50) {
            lineNum++;
            fullContent.append(line).append("\n");
            line = line.trim();

            if (line.isEmpty()) continue;

            // Look for PAN pattern: 5 letters + 4 digits + 1 letter + 1 digit + 1 letter + 1 digit
            // Example: AAAPA1234A1
            if (metadata.get("pan") == null) {
                java.util.regex.Pattern panPattern = java.util.regex.Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z][0-9][A-Z][0-9]");
                java.util.regex.Matcher panMatcher = panPattern.matcher(line);
                if (panMatcher.find()) {
                    metadata.put("pan", panMatcher.group());
                    log.debug("Found PAN: {} in line {}", metadata.get("pan"), lineNum);
                }
            }

            // Look for investor name - check multiple patterns
            if (metadata.get("investorName") == null) {
                // Pattern 1: "Folio:" or "Folio Number:" followed by name
                if (line.toLowerCase().matches(".*folio.*:.*")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length > 1) {
                        String content = parts[1].trim();
                        // Try to extract name from the next part
                        if (!content.isEmpty() && !content.matches("[0-9]+") && !content.matches("[A-Z0-9]+")) {
                            metadata.put("investorName", content);
                            log.debug("Found investor name from folio pattern: {}", content);
                        }
                    }
                }
                // Pattern 2: "Account Holder" or "Investor Name"
                else if (line.toLowerCase().contains("account holder") || line.toLowerCase().contains("investor")) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String name = parts[1].trim();
                            if (!name.isEmpty() && !name.matches("[0-9]+")) {
                                metadata.put("investorName", name);
                                log.debug("Found investor name from account holder pattern: {}", name);
                            }
                        }
                    }
                }
                // Pattern 3: "Name=" format
                else if (line.toLowerCase().contains("name=") || line.toLowerCase().contains("name :")) {
                    String[] parts = line.split("[=:]", 2);
                    if (parts.length > 1) {
                        String name = parts[1].trim();
                        if (!name.isEmpty() && !name.matches("[0-9]+")) {
                            metadata.put("investorName", name);
                            log.debug("Found investor name from name= pattern: {}", name);
                        }
                    }
                }
            }

            // If both found, we can stop reading
            if (metadata.get("pan") != null && metadata.get("investorName") != null) {
                log.debug("Both PAN and investor name found, stopping metadata extraction");
                break;
            }
        }

        // Additional fallback: if we have PAN and investor name wasn't found,
        // try looking for it in the full content with a broader search
        if (metadata.get("pan") != null && metadata.get("investorName") == null) {
            String content = fullContent.toString();
            // Look for common name indicators followed by actual name
            java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile(
                    "(?i)(account\\s+holder|investor\\s+name|name|folio)\\s*[:=]?\\s*([A-Za-z][A-Za-z\\s.'-]*[A-Za-z])"
            );
            java.util.regex.Matcher nameMatcher = namePattern.matcher(content);
            if (nameMatcher.find()) {
                String name = nameMatcher.group(2).trim();
                if (!name.isEmpty() && !name.matches("[0-9]+")) {
                    metadata.put("investorName", name);
                    log.debug("Found investor name from fallback pattern: {}", name);
                }
            }
        }
    }

    /**
     * Extract metadata fields from an InputStream (for ZIP-extracted files).
     * Supports extraction of PAN and investor name based on file content.
     *
     * @param inputStream the input stream from which metadata needs to be extracted
     * @param rtaName     the RTA name, used for logging and processing rules
     * @return a map containing extracted metadata fields: pan, investorName
     * @throws IOException if an error occurs during metadata extraction
     */
    private Map<String, String> extractMetadataFromFile(InputStream inputStream, String rtaName) throws IOException {
        Map<String, String> metadata = new HashMap<>();

        // Basic validation
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream is required for metadata extraction");
        }

        log.info("Extracting metadata from input stream (RTA: {})", rtaName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            processExtractedData(reader, metadata);
        }

        // If we couldn't extract metadata, use defaults
        if (metadata.get("pan") == null) {
            log.warn("PAN not found in input stream");
            metadata.put("pan", "UNKNOWN");
        }

        if (metadata.get("investorName") == null) {
            log.warn("Investor name not found in input stream");
            metadata.put("investorName", "Unknown Investor");
        }

        log.info("Extracted metadata - PAN: {}, Investor Name: {}", metadata.get("pan"), metadata.get("investorName"));
        return metadata;
    }
}
