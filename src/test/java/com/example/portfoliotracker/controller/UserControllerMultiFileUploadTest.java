package com.example.portfoliotracker.controller;

import com.example.portfoliotracker.entity.PortfolioUser;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.PortfolioUserRepository;
import com.example.portfoliotracker.service.TransactionIngestService;
import com.example.portfoliotracker.service.ZipHandlerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerMultiFileUploadTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TransactionIngestService ingestService;

    @MockBean
    private AmfiImportRepository importRepository;

    @MockBean
    private PortfolioUserRepository userRepository;

    @Test
    void uploadMultipleFiles_WithTransactionAndValuationFiles() throws Exception {
        // Setup
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Create test files
        MockMultipartFile transactionFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                "scheme_code,units,amount\n0P000088UP,100,10000\n".getBytes()
        );

        MockMultipartFile valuationFile = new MockMultipartFile(
                "valuationFile",
                "CurrentValuationABC123.txt",
                "text/plain",
                "scheme_code,units,currentValue\n0P000088UP,100,15000\n".getBytes()
        );

        // Execute
        mvc.perform(multipart("/api/users/upload-files")
                .file(transactionFile)
                .file(valuationFile)
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.uploadMode").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.filesProcessed").value(2))
                .andExpect(jsonPath("$.message").value(containsString("uploaded and queued")));
    }

    @Test
    void uploadMultipleFiles_WithTransactionFileOnly() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        MockMultipartFile transactionFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                "data".getBytes()
        );

        mvc.perform(multipart("/api/users/upload-files")
                .file(transactionFile)
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.uploadMode").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.filesProcessed").value(1));
    }

    @Test
    void uploadMultipleFiles_WithValuationFileOnly() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        MockMultipartFile valuationFile = new MockMultipartFile(
                "valuationFile",
                "CurrentValuation.txt",
                "text/plain",
                "data".getBytes()
        );

        mvc.perform(multipart("/api/users/upload-files")
                .file(valuationFile)
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.uploadMode").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.filesProcessed").value(1));
    }

    @Test
    void uploadMultipleFiles_WithZipFile() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Create ZIP file with new naming convention
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipOutput)) {
            ZipEntry transEntry = new ZipEntry("ABC123.txt");
            zos.putNextEntry(transEntry);
            zos.write("data".getBytes());
            zos.closeEntry();

            ZipEntry valEntry = new ZipEntry("CurrentValuationABC123.txt");
            zos.putNextEntry(valEntry);
            zos.write("data".getBytes());
            zos.closeEntry();
        }

        // Mock ZIP extraction
        ZipHandlerService.ExtractedFiles extracted = new ZipHandlerService.ExtractedFiles(
                null, null, "ABC123.txt", "CurrentValuationABC123.txt"
        ) {
            @Override
            public boolean hasTransactionFile() { return true; }
            @Override
            public boolean hasValuationFile() { return true; }
        };

        when(ingestService.extractFilesFromZip(any())).thenReturn(extracted);

        MockMultipartFile zipFile = new MockMultipartFile(
                "zipFile",
                "portfolio.zip",
                "application/zip",
                zipOutput.toByteArray()
        );

        mvc.perform(multipart("/api/users/upload-files")
                .file(zipFile)
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.uploadMode").value("ZIP"))
                .andExpect(jsonPath("$.filesProcessed").value(is(2)));
    }

    @Test
    void uploadMultipleFiles_NoFilesProvided() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        mvc.perform(multipart("/api/users/upload-files")
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        containsString("At least one file must be provided")));
    }

    @Test
    void uploadMultipleFiles_UserNotFound() throws Exception {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        MockMultipartFile transactionFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                "data".getBytes()
        );

        mvc.perform(multipart("/api/users/upload-files")
                .file(transactionFile)
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }

    @Test
    void uploadMultipleFiles_InvalidEmail() throws Exception {
        MockMultipartFile transactionFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                "data".getBytes()
        );

        mvc.perform(multipart("/api/users/upload-files")
                .file(transactionFile)
                .param("email", "not-an-email")
                .param("rtaName", "CAMS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("must be valid")));
    }

    @Test
    void uploadMultipleFiles_EmptyRtaName() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        MockMultipartFile transactionFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                "data".getBytes()
        );

        mvc.perform(multipart("/api/users/upload-files")
                .file(transactionFile)
                .param("email", email)
                .param("rtaName", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void uploadMultipleFiles_WithEmptyFiles() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Create empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                new byte[0]
        );

        // Empty file should be treated as "not provided"
        mvc.perform(multipart("/api/users/upload-files")
                .file(emptyFile)
                .param("email", email)
                .param("rtaName", "CAMS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        containsString("At least one file must be provided")));
    }

    @Test
    void uploadMultipleFiles_PreserveImportId() throws Exception {
        String email = "user@example.com";
        PortfolioUser user = new PortfolioUser();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        MockMultipartFile transactionFile = new MockMultipartFile(
                "transactionFile",
                "ABC123.txt",
                "text/plain",
                "data".getBytes()
        );

        long existingImportId = 999L;

        mvc.perform(multipart("/api/users/upload-files")
                .file(transactionFile)
                .param("email", email)
                .param("rtaName", "CAMS")
                .param("importId", String.valueOf(existingImportId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.importId").value(is((int) existingImportId)));
    }
}

