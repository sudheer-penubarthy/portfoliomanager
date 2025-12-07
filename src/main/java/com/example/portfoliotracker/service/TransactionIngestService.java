package com.example.portfoliotracker.service;

import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;

public interface TransactionIngestService {
    @Transactional
    Long ingestCsvForUser(String email, String rtaName, InputStream csvStream, Long importId, boolean isValuationFile) throws Exception;

    @Transactional
    void recomputeHoldingsForUser(Long userId);

    Map<String, Object> getUserSnapshotByEmail(String email);
}
