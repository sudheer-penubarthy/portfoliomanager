package com.example.portfoliotracker.service.impl;

import com.example.portfoliotracker.entity.PortfolioUser;
import com.example.portfoliotracker.entity.UserHolding;
import com.example.portfoliotracker.entity.UserTransaction;
import com.example.portfoliotracker.enums.Status;
import com.example.portfoliotracker.repository.*;
import com.example.portfoliotracker.service.CamsStreamParser;
import com.example.portfoliotracker.service.TransactionIngestService;
import com.example.portfoliotracker.service.ZipHandlerService;
import com.example.portfoliotracker.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TransactionIngestServiceImpl implements TransactionIngestService {
    private final PortfolioUserRepository userRepo;
    private final ExternalSchemeMapRepository mapRepo;
    private final UserTransactionRepository txnRepo;
    private final UserHoldingRepository holdingRepo;
    private final AmfiSchemeRepository schemeRepo;
    private final AmfiNavRepository navRepo;
    private final JdbcTemplate jdbc;
    private final AmfiImportRepository importRepository;
    private final CamsStreamParser camsStreamParser;
    private final ZipHandlerService zipHandlerService;

    public TransactionIngestServiceImpl(PortfolioUserRepository userRepo, ExternalSchemeMapRepository mapRepo, UserTransactionRepository txnRepo, UserHoldingRepository holdingRepo, AmfiSchemeRepository schemeRepo, AmfiNavRepository navRepo, AmfiImportRepository importRepository, JdbcTemplate jdbc, CamsStreamParser camsStreamParser, ZipHandlerService zipHandlerService) {
        this.camsStreamParser = camsStreamParser;
        this.userRepo = userRepo;
        this.mapRepo = mapRepo;
        this.txnRepo = txnRepo;
        this.holdingRepo = holdingRepo;
        this.schemeRepo = schemeRepo;
        this.navRepo = navRepo;
        this.jdbc = jdbc;
        this.importRepository = importRepository;
        this.zipHandlerService = zipHandlerService;
    }

    /**
     * High-level: accept an InputStream (CSV/TSV) from CAMS/KFin or generic CSV.
     * Minimal parser: expects columns [sourceReference, txnDate(yyyy-MM-dd), rtaCode, txnType, units, amount]
     */
    public void ingestCsvForUser(
            String email,
            String rtaName,
            byte[] fileBytes,
            Long importId,
            boolean isValuation) throws Exception {

        // Hard safety guard – ZIP must never reach here
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Empty file content received");
        }
        if (fileBytes.length >= 2 &&
                fileBytes[0] == 'P' &&
                fileBytes[1] == 'K') {
            throw new IllegalArgumentException(
                    "ZIP binary passed to TransactionIngestService");
        }

        Charset charset = StringUtil.detectCharset(fileBytes); // the one we already fixed
        log.info("Using charset {} for ingestion (valuation={})",
                charset.name(), isValuation);

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new ByteArrayInputStream(fileBytes), charset))) {
            if (isValuation) {
                ingestValuationFile(reader, email, rtaName, importId);
            } else {
                ingestTransactionFile(reader, email, rtaName, importId);
            }

        }
        log.info("Ingestion completed for user: {} (id={}), RTA: {}, importId: {}. Processed: {}, Inserted: {}, Skipped: {}", email, user.getId(), rtaName, importId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
        importRepository.updateProgress(importId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
        log.info("Marking importId: {} as COMPLETED", importId);
        importRepository.markCompleted(importId, Status.COMPLETED);

        return user.getId();
    }

/*
        if (isValuationFile) {
            insertValuationFile(rtaName, csvStream, importId, user, rowsProcessed, BATCH, rowsInserted, finalImportId, rowsSkipped);
        } else {
            insertTransactionFileAndRecomputeHoldings(rtaName, csvStream, importId, finalImportId, user, rowsProcessed, rowsSkipped, rowsInserted, BATCH);
        }*/

    }

    private void ingestTransactionFile(
            BufferedReader reader,
            String email,
            String rtaName,
            Long importId) throws Exception {

        String line;
        int lineNo = 0;

        while ((line = reader.readLine()) != null) {
            lineNo++;

            // skip header if needed
            if (lineNo == 1) {
                continue;
            }

            String[] cols = line.split("\t", -1);

            // existing validation / parsing logic here
            // persist transactions
        }
    }

    private void ingestValuationFile(
            BufferedReader reader,
            String email,
            String rtaName,
            Long importId) throws Exception {

        String line;
        int lineNo = 0;

        while ((line = reader.readLine()) != null) {
            lineNo++;

            // skip header if needed
            if (lineNo == 1) {
                continue;
            }

            String[] cols = line.split("\t", -1);

            // existing validation / parsing logic here

            // persist holdings
        }
    }


    private void insertTransactionFileAndRecomputeHoldings(String rtaName, InputStream csvStream, Long importId, Long finalImportId, PortfolioUser user, AtomicInteger rowsProcessed, AtomicInteger rowsSkipped, AtomicInteger rowsInserted, int BATCH) throws Exception {
        // transaction file: stream and batch insert transactions, then recompute holdings
        log.info("Starting batch import for non -valuation transaction file");
        List<UserTransaction> tchunk = new ArrayList<>();
        camsStreamParser.streamTransactions(csvStream, finalImportId, user.getId(), rtaName, txn -> {
            rowsProcessed.incrementAndGet();
            // dedupe check (lightweight): skip if sourceReference exists
            if (txn.getSourceReference() != null && txnRepo.existsByUserIdAndSourceReference(user.getId(), txn.getSourceReference())) {
                rowsSkipped.incrementAndGet();
                if (rowsProcessed.get() % 500 == 0)
                    importRepository.updateProgress(finalImportId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
                return; // skip
            }
            tchunk.add(txn);
            if (tchunk.size() >= BATCH) {
                log.info("Inserting chunk of {} transactions", tchunk.size());
                batchInsertUserTransactions(tchunk);
                rowsInserted.addAndGet(tchunk.size());
                importRepository.updateProgress(finalImportId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
                tchunk.clear();
            }
        });
        if (!tchunk.isEmpty()) {
            log.info("Inserting final chunk of {} transactions", tchunk.size());
            batchInsertUserTransactions(tchunk);
            log.info("Final chunk inserted");
            rowsInserted.addAndGet(tchunk.size());
            importRepository.updateProgress(importId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
        }

        // recompute holdings after transactions are saved
        log.info("Recomputing holdings for user id: {}", user.getId());
        recomputeHoldingsForUser(user.getId());
    }

    private void insertValuationFile(String rtaName, InputStream csvStream, Long importId, PortfolioUser user, AtomicInteger rowsProcessed, int BATCH, AtomicInteger rowsInserted, Long finalImportId, AtomicInteger rowsSkipped) throws Exception {
        // if valuation snapshot provided, stream and upsert holdings directly
        List<UserHolding> chunk = new ArrayList<>();
        camsStreamParser.streamValuation(csvStream, user.getId(), rtaName, h -> {
            rowsProcessed.incrementAndGet();
            chunk.add(h);
            if (chunk.size() >= BATCH) {
                // upsert chunk: delete existing matching user holdings for these scheme codes, then insert
                List<String> schemeCodes = chunk.stream().map(UserHolding::getSchemeCode).distinct().toList();
                // delete existing for this user and these scheme codes
                jdbc.update("DELETE FROM user_holding WHERE user_id = ? AND scheme_code IN (" + String.join(",", Collections.nCopies(schemeCodes.size(), "?")) + ")", buildDeleteParams(user.getId(), schemeCodes));
                // batch insert
                batchInsertHoldings(chunk);
                rowsInserted.addAndGet(chunk.size());
                importRepository.updateProgress(finalImportId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
                chunk.clear();
            }
        });
        if (!chunk.isEmpty()) {
            List<String> schemeCodes = chunk.stream().map(UserHolding::getSchemeCode).distinct().toList();
            jdbc.update("DELETE FROM user_holding WHERE user_id = ? AND scheme_code IN (" + String.join(",", Collections.nCopies(schemeCodes.size(), "?")) + ")", buildDeleteParams(user.getId(), schemeCodes));
            batchInsertHoldings(chunk);
            rowsInserted.addAndGet(chunk.size());
            importRepository.updateProgress(importId, rowsProcessed.get(), rowsInserted.get(), rowsSkipped.get());
        }
    }

    private Long persistFile(String rtaName) {
        Long importId;
        var imp = com.example.portfoliotracker.entity.AmfiImport.builder().fileName("upload.csv").sourceUrl(rtaName).status(Status.PROCESSING).createdAt(LocalDateTime.now()).build();
        imp = importRepository.save(imp);
        importId = imp.getId();
        return importId;
    }

    // helper to build delete params for jdbc.update: [userId, sc1, sc2, ...]
    private Object[] buildDeleteParams(Long userId, java.util.List<String> schemeCodes) {
        Object[] params = new Object[1 + schemeCodes.size()];
        params[0] = userId;
        for (int i = 0; i < schemeCodes.size(); i++) params[i + 1] = schemeCodes.get(i);
        return params;
    }

    private void batchInsertHoldings(java.util.List<UserHolding> holders) {
        if (holders.isEmpty()) return;
        final String sql = "INSERT INTO user_holding (user_id, scheme_code, units, avg_cost_per_unit, total_cost, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        final int batch = 500;
        for (int i = 0; i < holders.size(); i += batch) {
            int end = Math.min(i + batch, holders.size());
            List<UserHolding> slice = holders.subList(i, end);
            jdbc.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int idx) throws java.sql.SQLException {
                    UserHolding h = slice.get(idx);
                    ps.setLong(1, h.getUserId());
                    ps.setString(2, h.getSchemeCode());
                    ps.setBigDecimal(3, h.getUnits());
                    ps.setBigDecimal(4, h.getAvgCostPerUnit());
                    ps.setBigDecimal(5, h.getTotalCost());
                    if (null == h.getLastUpdated()) h.setLastUpdated(LocalDateTime.now());
                    ps.setTimestamp(6, Timestamp.valueOf(h.getLastUpdated()));
                }

                @Override
                public int getBatchSize() {
                    return slice.size();
                }
            });
        }
    }

    private void batchInsertUserTransactions(List<UserTransaction> txns) {
        if (txns.isEmpty()) return;
        final String sql = "INSERT INTO user_transaction (user_id, txn_source, source_reference, txn_date, scheme_code, txn_type, units, amount, price_per_unit, remarks, import_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int batch = 500;
        for (int i = 0; i < txns.size(); i += batch) {
            int end = Math.min(i + batch, txns.size());
            List<UserTransaction> slice = txns.subList(i, end);
            jdbc.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int idx) throws SQLException {
                    UserTransaction t = slice.get(idx);
                    ps.setLong(1, t.getUserId());
                    ps.setString(2, t.getTxnSource());
                    ps.setString(3, t.getSourceReference());
                    ps.setDate(4, java.sql.Date.valueOf(t.getTxnDate()));
                    ps.setString(5, t.getSchemeCode());
                    ps.setString(6, t.getTxnType().name());
                    ps.setBigDecimal(7, t.getUnits());
                    ps.setBigDecimal(8, t.getAmount());
                    ps.setBigDecimal(9, t.getPricePerUnit());
                    ps.setString(10, t.getRemarks());
                    if (t.getImportId() != null) ps.setLong(11, t.getImportId());
                    else ps.setNull(11, java.sql.Types.BIGINT);
                    ps.setTimestamp(12, java.sql.Timestamp.valueOf(t.getCreatedAt()));
                }


                @Override
                public int getBatchSize() {
                    return slice.size();
                }
            });
        }
    }

    /**
     * Recompute holdings from user_transaction aggregation and upsert into user_holding.
     */
    @Transactional
    @Override
    public void recomputeHoldingsForUser(Long userId) {
        // aggregate units and total cost per scheme (BUY adds units/amount, SELL subtracts)
        String sql = "SELECT scheme_code, SUM(CASE WHEN txn_type='BUY' THEN units WHEN txn_type IN ('SELL') THEN -units ELSE 0 END) AS units, " + "SUM(CASE WHEN txn_type='BUY' THEN amount WHEN txn_type IN ('SELL') THEN -amount ELSE 0 END) AS net_amount " + "FROM user_transaction WHERE user_id = ? GROUP BY scheme_code";


        List<Map<String, Object>> rows = jdbc.queryForList(sql, userId);


        List<UserHolding> holders = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String schemeCode = (String) r.get("scheme_code");
            java.math.BigDecimal units = (java.math.BigDecimal) r.get("units");
            java.math.BigDecimal netAmount = (java.math.BigDecimal) r.get("net_amount");
            if (units == null || units.compareTo(java.math.BigDecimal.ZERO) == 0) {
                // remove holding if exists
                Optional<UserHolding> ex = holdingRepo.findByUserIdAndSchemeCode(userId, schemeCode);
                ex.ifPresent(h -> holdingRepo.delete(h));
                continue;
            }
            BigDecimal avgCost = netAmount.divide(units, 8, RoundingMode.HALF_UP);
            UserHolding h = UserHolding.builder().userId(userId).schemeCode(schemeCode).units(units).totalCost(netAmount).avgCostPerUnit(avgCost).lastUpdated(LocalDateTime.now()).build();
            holders.add(h);
        }
        // upsert holdings: delete existing for user and re-insert (simple, safe)
        List<UserHolding> existing = holdingRepo.findByUserId(userId);
        if (!existing.isEmpty()) holdingRepo.deleteAll(existing);
        if (!holders.isEmpty()) holdingRepo.saveAll(holders);
    }


    /**
     * Snapshot: current holdings + latest NAV + PnL
     */
    @Override
    public Map<String, Object> getUserSnapshotByEmail(String email) {
        Optional<PortfolioUser> ou = userRepo.findByEmail(email);
        if (!ou.isPresent()) return Collections.emptyMap();
        PortfolioUser user = ou.get();
        List<UserHolding> holds = holdingRepo.findByUserId(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal totalCurrent = BigDecimal.ZERO, totalCost = BigDecimal.ZERO;
        for (UserHolding h : holds) {
            // latest nav
            Optional<com.example.portfoliotracker.entity.AmfiNav> navOpt = navRepo.findTopBySchemeCodeOrderByNavDateDesc(h.getSchemeCode());
            BigDecimal latestNav = navOpt.map(com.example.portfoliotracker.entity.AmfiNav::getNavValue).orElse(BigDecimal.ZERO);
            BigDecimal currentValue = h.getUnits().multiply(latestNav);
            BigDecimal pnl = currentValue.subtract(h.getTotalCost());
            totalCurrent = totalCurrent.add(currentValue);
            totalCost = totalCost.add(h.getTotalCost());
            Map<String, Object> item = new HashMap<>();
            item.put("schemeCode", h.getSchemeCode());
            item.put("units", h.getUnits());
            item.put("avgCost", h.getAvgCostPerUnit());
            item.put("totalCost", h.getTotalCost());
            item.put("latestNav", latestNav);
            item.put("currentValue", currentValue);
            item.put("pnl", pnl);
            result.add(item);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("user", user);
        out.put("holdings", result);
        out.put("totalCurrentValue", totalCurrent);
        out.put("totalCost", totalCost);
        out.put("totalPnl", totalCurrent.subtract(totalCost));
        return out;
    }

    /**
     * Extract files from a ZIP archive using ZipHandlerService.
     */
    @Override
    public ZipHandlerService.ExtractedFiles extractFilesFromZip(InputStream zipInputStream) throws Exception {
        log.debug("Delegating ZIP extraction to ZipHandlerService");
        return zipHandlerService.extractFromZip(zipInputStream);
    }

    /**
     * Extract files from a password-protected ZIP archive using ZipHandlerService.
     */
    @Override
    public ZipHandlerService.ExtractedFiles extractFilesFromZip(InputStream zipInputStream, String password) throws Exception {
        log.debug("Delegating password-protected ZIP extraction to ZipHandlerService");
        if (password == null || password.isEmpty()) {
            log.warn("Password provided for ZIP extraction but is empty or null, attempting non-protected extraction");
            return zipHandlerService.extractFromZip(zipInputStream);
        }
        return zipHandlerService.extractFromPasswordProtectedZip(zipInputStream, password);
    }
}
