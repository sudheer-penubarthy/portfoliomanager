package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.entity.PortfolioUser;
import com.example.portfoliotracker.enums.Status;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.PortfolioUserRepository;
import com.example.portfoliotracker.service.TransactionIngestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {


    private final TransactionIngestService ingestService;
    private final AmfiImportRepository importRepository;
    private final PortfolioUserRepository userRepository;


    public UserController(TransactionIngestService ingestService, AmfiImportRepository importRepository, PortfolioUserRepository userRepository) {
        this.userRepository = userRepository;
        this.ingestService = ingestService;
        this.importRepository = importRepository;
    }


    @PostMapping(value = "/upload-transactions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadTransactions(@RequestParam String email,
                                                @RequestParam String rtaName,
                                                @RequestPart MultipartFile file,
                                                @RequestParam boolean isValuationFile,
                                                @RequestParam(required = false) Long importId) {
        try {
            // if importId not provided, create a new import
            if (importId == null) {
                var imp = com.example.portfoliotracker.entity.AmfiImport.builder()
                        .fileName(file.getOriginalFilename())
                        .sourceUrl(rtaName)
                        .status(Status.PROCESSING)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                imp = importRepository.save(imp);
                importId = imp.getId();
            }

            log.info("Starting ingestion for user: {}, RTA: {}, ImportId: {}", email, rtaName, importId);
            ingestService.ingestCsvForUser(email, rtaName, file.getInputStream(), importId,isValuationFile);
            return ResponseEntity.accepted().body(Map.of("importId", importId));
        } catch (Exception ex) {
            log.debug("Error during ingestion for user: {}, RTA: {}: {}", email, rtaName, ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }


    @GetMapping("/{email}/snapshot")
    public ResponseEntity<?> snapshot(@PathVariable String email) {
        Map<String, Object> snap = ingestService.getUserSnapshotByEmail(email);
        if (snap.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(snap);
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<List<PortfolioUser>> getAllUsers() {
        List<PortfolioUser> users = userRepository.findAll();
        return ResponseEntity.ok().body(users);
    }

}
