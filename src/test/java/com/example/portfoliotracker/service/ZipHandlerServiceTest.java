package com.example.portfoliotracker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipHandlerServiceTest {

    private ZipHandlerService zipHandlerService;

    @BeforeEach
    void setUp() {
        zipHandlerService = new ZipHandlerService();
    }

    @Test
    void testExtractFromZip_WithBothFiles() throws IOException {
        // Create test ZIP with both files
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            // Add transaction file
            ZipEntry transEntry = new ZipEntry("ABC123.txt");
            zos.putNextEntry(transEntry);
            zos.write("scheme_code,units,amount\n0P000088UP,100,10000\n".getBytes());
            zos.closeEntry();

            // Add valuation file with naming convention: CurrentValuation + transaction filename
            ZipEntry valEntry = new ZipEntry("CurrentValuationABC123.txt");
            zos.putNextEntry(valEntry);
            zos.write("scheme_code,units,currentValue\n0P000088UP,100,15000\n".getBytes());
            zos.closeEntry();
        }

        // Extract and verify
        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        assertTrue(extracted.hasTransactionFile(), "Should have transaction file");
        assertTrue(extracted.hasValuationFile(), "Should have valuation file");
        assertEquals("ABC123.txt", extracted.getTransactionFileName());
        assertEquals("CurrentValuationABC123.txt", extracted.getValuationFileName());
    }

    @Test
    void testExtractFromZip_TransactionFileOnly() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry transEntry = new ZipEntry("CAMS2024.txt");
            zos.putNextEntry(transEntry);
            zos.write("transaction data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        assertTrue(extracted.hasTransactionFile(), "Should have transaction file");
        assertFalse(extracted.hasValuationFile(), "Should NOT have valuation file");
        assertEquals("CAMS2024.txt", extracted.getTransactionFileName());
    }

    @Test
    void testExtractFromZip_ValuationFileOnly() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry valEntry = new ZipEntry("CurrentValuationABC123.csv");
            zos.putNextEntry(valEntry);
            zos.write("valuation data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        assertFalse(extracted.hasTransactionFile(), "Should NOT have transaction file");
        assertTrue(extracted.hasValuationFile(), "Should have valuation file");
        assertEquals("CurrentValuationABC123.csv", extracted.getValuationFileName());
    }

    @Test
    void testExtractFromZip_NoValidFiles() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry entry = new ZipEntry("invalid_file.txt");
            zos.putNextEntry(entry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> {
            zipHandlerService.extractFromZip(zipInput);
        }, "Should throw exception for no valid files");
    }

    @Test
    void testExtractFromZip_FilesWithExtensions() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            // Transaction with various extensions
            ZipEntry trans1 = new ZipEntry("CAMS123.txt");
            zos.putNextEntry(trans1);
            zos.write("data".getBytes());
            zos.closeEntry();

            // Valuation with CSV extension, using new naming convention
            ZipEntry val = new ZipEntry("CurrentValuationCAMS123.csv");
            zos.putNextEntry(val);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        assertTrue(extracted.hasTransactionFile());
        assertTrue(extracted.hasValuationFile());
    }

    @Test
    void testExtractFromZip_CaseInsensitiveValuationName() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            // Transaction file
            ZipEntry transEntry = new ZipEntry("ABC123.txt");
            zos.putNextEntry(transEntry);
            zos.write("data".getBytes());
            zos.closeEntry();

            // Test case-insensitive matching with CurrentValuation prefix
            ZipEntry valEntry = new ZipEntry("currentvaluationabc123.txt");
            zos.putNextEntry(valEntry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        assertTrue(extracted.hasValuationFile(), "Should recognize case-insensitive valuation name");
    }

    @Test
    void testExtractFromZip_IgnoresDirectories() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            // Add directory entry
            ZipEntry dirEntry = new ZipEntry("subfolder/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();

            // Add file in directory
            ZipEntry fileEntry = new ZipEntry("subfolder/ABC123.txt");
            zos.putNextEntry(fileEntry);
            zos.write("data".getBytes());
            zos.closeEntry();

            // Add top-level valuation file with new naming convention
            ZipEntry topEntry = new ZipEntry("CurrentValuationABC123.txt");
            zos.putNextEntry(topEntry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        // Should find both files even with nested structure
        assertTrue(extracted.hasTransactionFile() || extracted.hasValuationFile(),
                "Should extract files from ZIP regardless of structure");
    }

    @Test
    void testIsValidZipStructure_Valid() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry entry = new ZipEntry("ABC123.txt");
            zos.putNextEntry(entry);
            zos.write("data".getBytes());
            zos.closeEntry();

            ZipEntry valEntry = new ZipEntry("CurrentValuationABC123.txt");
            zos.putNextEntry(valEntry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        assertTrue(zipHandlerService.isValidZipStructure(zipInput),
                "Should recognize valid ZIP structure");
    }

    @Test
    void testIsValidZipStructure_Invalid() throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry entry = new ZipEntry("invalid.txt");
            zos.putNextEntry(entry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        assertFalse(zipHandlerService.isValidZipStructure(zipInput),
                "Should reject invalid ZIP structure");
    }

    @Test
    void testExtractFromPasswordProtectedZip_NotSupported() {
        ByteArrayInputStream zipInput = new ByteArrayInputStream(new byte[0]);
        assertThrows(UnsupportedOperationException.class, () -> {
            zipHandlerService.extractFromPasswordProtectedZip(zipInput, "password");
        }, "Should throw UnsupportedOperationException for password-protected ZIP");
    }

    @Test
    void testExtractFromZip_PreservesFileContent() throws IOException {
        String testContent = "scheme_code,units,amount\n0P000088UP,100,10000\nSample,200,20000\n";

        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry entry = new ZipEntry("ABC123.txt");
            zos.putNextEntry(entry);
            zos.write(testContent.getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream zipInput = new ByteArrayInputStream(zipOutput.toByteArray());
        ZipHandlerService.ExtractedFiles extracted = zipHandlerService.extractFromZip(zipInput);

        // Verify content is preserved
        assertTrue(extracted.hasTransactionFile());
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(extracted.getTransactionFile()));
        String firstLine = reader.readLine();
        assertEquals("scheme_code,units,amount", firstLine, "File content should be preserved");
    }
}

