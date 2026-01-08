package com.example.portfoliotracker.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Service for handling ZIP file operations and multi-file extraction.
 * Supports password-protected ZIP files for secure file uploads.
 */
@Slf4j
@Service
public class ZipHandlerService {


    private static final Pattern TRANSACTION_FILE_PATTERN = Pattern.compile("^[A-Za-z0-9]+\\.txt$");

    private static final String VALUATION_PREFIX = "CurrentValuation";

    /**
     * Container for extracted files from a ZIP archive.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ExtractedFiles {
        private byte[] transactionFile;
        private byte[] valuationFile;

        private String transactionFileName;
        private String valuationFileName;
    }

    /**
     * Extract files from a standard (non-encrypted) ZIP archive.
     * Expects files named:
     * - Transaction file: alphanumeric string (e.g., "ABC123.txt")
     * - Valuation file: "CurrentValuation" + transaction filename (e.g., "CurrentValuationABC123.txt")
     *
     * @param zipInputStream the ZIP file input stream
     * @return ExtractedFiles containing the extracted file streams
     * @throws IOException if extraction fails
     */
    public ExtractedFiles extractFromZip(InputStream zipInputStream) throws IOException {
        log.debug("Extracting files from ZIP archive");
        Map<String, byte[]> files = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String name = Paths.get(entry.getName()).getFileName().toString();
                byte[] data = zis.readAllBytes();

                log.info("Extracted ZIP entry: {} ({} bytes)", name, data.length);

                if (data.length > 0 && data[0] == 'P' && data[1] == 'K') {
                    throw new IllegalArgumentException("Nested ZIP not allowed");
                }


                files.put(name, data);
                zis.closeEntry();
            }
        }

        validateZipFiles(files);

        return buildExtractedFiles(files);
    }


    private void validateZipFiles(Map<String, byte[]> files) {

        if (files.size() != 2) {
            throw new IllegalArgumentException("ZIP must contain exactly 2 files. Found: " + files.size());
        }

        String transactionFile = null;

        for (String name : files.keySet()) {
            if (TRANSACTION_FILE_PATTERN.matcher(name).matches() && !name.startsWith(VALUATION_PREFIX)) {
                transactionFile = name;
                break;
            }
        }

        if (transactionFile == null) {
            throw new IllegalArgumentException("Transaction file not found. Expected <alphanumeric>.txt");
        }

        String expectedValuation = VALUATION_PREFIX + transactionFile;

        if (!files.containsKey(expectedValuation)) {
            throw new IllegalArgumentException("Valuation file not found. Expected: " + expectedValuation);
        }
    }

    private ZipHandlerService.ExtractedFiles buildExtractedFiles(Map<String, byte[]> files) {

        ZipHandlerService.ExtractedFiles extracted = new ZipHandlerService.ExtractedFiles();

        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().startsWith(VALUATION_PREFIX)) {
                extracted.setValuationFile(e.getValue());
            } else {
                extracted.setTransactionFile(e.getValue());
            }
        }

        return extracted;
    }

}

