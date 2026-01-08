package com.example.portfoliotracker.service;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface TransactionIngestService {
    @Transactional
    public void ingestCsvForUser(
            String email,
            String rtaName,
            byte[] fileBytes,
            Long importId,
            boolean isValuation) throws Exception;


    @Transactional
    void recomputeHoldingsForUser(Long userId);

    Map<String, Object> getUserSnapshotByEmail(String email);

    /**
     * Extract files from a standard (non-encrypted) ZIP archive.
     * @param zipInputStream the ZIP file input stream
     * @return ExtractedFiles containing transaction and/or valuation file streams
     * @throws Exception if extraction fails
     */
    ZipHandlerService.ExtractedFiles extractFilesFromZip(InputStream zipInputStream) throws Exception;

    /**
     * Extract files from a password-protected ZIP archive.
     * @param zipInputStream the encrypted ZIP file input stream
     * @param password the password to decrypt the archive
     * @return ExtractedFiles containing transaction and/or valuation file streams
     * @throws Exception if extraction fails or password is invalid
     */
    ZipHandlerService.ExtractedFiles extractFilesFromZip(InputStream zipInputStream, String password) throws Exception;
}


