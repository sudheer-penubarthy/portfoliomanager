package com.sudheer.portfoliotracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Service to extract metadata (email, PAN, etc.) from uploaded CSV files
 */
@Slf4j
@Service
public class FileMetadataExtractor {

    /**
     * Extract email from transaction/valuation CSV file
     * Assumes email is in one of the common header columns
     *
     * @param fileBytes the file content as bytes
     * @return extracted email or null if not found
     */
    public String extractEmailFromCsv(byte[] fileBytes) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8)
            );

            // Read first few lines to find email
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 10) {
                lineCount++;

                // Check if line contains email-like pattern (simple check)
                if (line.contains("@")) {
                    String email = extractEmailFromLine(line);
                    if (email != null) {
                        log.debug("Extracted email from file: {}", email);
                        return email;
                    }
                }
            }

            reader.close();
            log.warn("No email found in CSV file");
            return null;

        } catch (IOException ex) {
            log.error("Error reading file to extract email: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extract email from a single line
     * Looks for pattern: email=...@... or similar
     *
     * @param line the line to search
     * @return extracted email or null
     */
    private String extractEmailFromLine(String line) {
        // Try to find email pattern
        String[] parts = line.split("[,\t|]");  // Split by common delimiters

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.contains("@") && trimmed.contains(".")) {
                // Basic email validation
                if (isValidEmail(trimmed)) {
                    return trimmed;
                }
            }
        }

        return null;
    }

    /**
     * Basic email validation
     *
     * @param email the email to validate
     * @return true if email looks valid
     */
    private boolean isValidEmail(String email) {
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailPattern);
    }

    /**
     * Extract PAN from transaction/valuation CSV file
     * Assumes PAN is in header columns or first few lines
     *
     * @param fileBytes the file content as bytes
     * @return extracted PAN or null if not found
     */
    public String extractPanFromCsv(byte[] fileBytes) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8)
            );

            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 10) {
                lineCount++;

                // PAN format: AAAPP0000A (10 alphanumeric characters)
                String pan = extractPanFromLine(line);
                if (pan != null) {
                    log.debug("Extracted PAN from file: {}", pan);
                    reader.close();
                    return pan;
                }
            }

            reader.close();
            log.warn("No PAN found in CSV file");
            return null;

        } catch (IOException ex) {
            log.error("Error reading file to extract PAN: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extract PAN from a single line
     *
     * @param line the line to search
     * @return extracted PAN or null
     */
    private String extractPanFromLine(String line) {
        String[] parts = line.split("[,\t|]");

        for (String part : parts) {
            String trimmed = part.trim().toUpperCase();
            // PAN format check: AAAPP0000A (10 chars, specific pattern)
            if (trimmed.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
                return trimmed;
            }
        }

        return null;
    }
}

