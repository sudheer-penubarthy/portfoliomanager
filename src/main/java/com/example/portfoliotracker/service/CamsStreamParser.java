package com.example.portfoliotracker.service;


import com.example.portfoliotracker.entity.ExternalSchemeMap;
import com.example.portfoliotracker.entity.UserHolding;
import com.example.portfoliotracker.entity.UserTransaction;
import com.example.portfoliotracker.enums.DateFormat;
import com.example.portfoliotracker.enums.TxnType;
import com.example.portfoliotracker.repository.ExternalSchemeMapRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stream-based CAMS parser that parses input streams row-by-row and calls a Consumer
 * for each parsed UserTransaction or UserHolding. This keeps memory usage constant
 * for very large files.
 */
@Component
@Slf4j
public class CamsStreamParser {

    private final ExternalSchemeMapRepository mapRepo;
    private static final Pattern MONTH_ABBR = Pattern.compile("(?i)\\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\b");

    public CamsStreamParser(ExternalSchemeMapRepository mapRepo) {
        this.mapRepo = mapRepo;
    }

    private LocalDate parseDateLenient(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        for (DateFormat df : DateFormat.values()) {
            try {
                return LocalDate.parse(s, df.getFormatter());
            } catch (Exception ignored) {
                log.trace("Ignored date parse error for format {}: {}", df.name(), s);
            }
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception ex) {
            log.error("Failed to parse date: {}", s);
        }
        return null;
    }

    private String normalizeMonthAbbr(String s) {
        Matcher m = MONTH_ABBR.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String mm = m.group().toLowerCase(Locale.ENGLISH);
            String repl = Character.toUpperCase(mm.charAt(0)) + mm.substring(1);
            m.appendReplacement(sb, repl);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private BigDecimal parseBigDecimalLenient(String s) {
        if (s == null) return null;
        s = s.trim().replaceAll("[ ,₹]", "");
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception ex) {
            log.debug("Failed to parse BigDecimal: {}", s);
            return null;
        }
    }

    private Map<String, Integer> headerMap(String headerLine, String sepRegex) {
        String[] cols = headerLine.split(sepRegex, -1);
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            String key = cols[i].trim().toLowerCase(Locale.ROOT).replaceAll("", "").replaceAll("", "");
            map.put(key, i);
        }
        return map;
    }

    private String readCol(String[] cols, Map<String, Integer> hm, String... candidates) {
        for (String c : candidates) {
            Integer i = hm.get(c.toLowerCase(Locale.ROOT));
            if (i != null && i >= 0 && i < cols.length) {
                String v = cols[i].trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    private String mapCamsTxnType(String camsType) {
        if (camsType == null) return "OTHER";
        String t = camsType.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return "OTHER";
        if (t.contains("purchase") || t.contains("buy") || t.contains("fresh") || t.contains("subscription"))
            return "BUY";
        if (t.contains("redemption") || t.contains("sell") || t.contains("repurchase")) return "SELL";
        if (t.contains("switch") && t.contains("in")) return "SWITCH_IN";
        if (t.contains("switch") && t.contains("out")) return "SWITCH_OUT";
        if (t.contains("dividend") || t.contains("drp") || t.contains("payout")) return "DIVIDEND";
        if (t.contains("sip") || t.contains("stp")) return "BUY";
        return t.toUpperCase(Locale.ROOT).replaceAll("\s+", "_");
    }

    /**
     * Stream parse transactions TSV and call consumer for each parsed UserTransaction.
     * The consumer receives a built UserTransaction (not persisted).
     */
    public void streamTransactions(InputStream is, Long importId, Long userId, String rtaName, Consumer<UserTransaction> consumer) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String header = br.readLine();
            if (header == null) return;
            Map<String, Integer> hm = headerMap(header, "\t");
            String line;
            int lineno = 1;
            while ((line = br.readLine()) != null) {
                lineno++;
                if (line.isBlank()) continue;
                String[] cols = line.split("\t", -1);
                try {
                    String mfName = readCol(cols, hm, "mf_name", "mf name", "amc name");
                    String pan = readCol(cols, hm, "pan");
                    String folio = readCol(cols, hm, "folio_number", "folio");
                    String productCode = readCol(cols, hm, "product_code", "schemecode", "product code");
                    String schemeName = readCol(cols, hm, "scheme_name", "scheme name", "scheme");
                    String tradeDate = readCol(cols, hm, "trade_date", "trade date");
                    String transactionType = readCol(cols, hm, "transaction_type", "transaction type");
                    String amountStr = readCol(cols, hm, "amount");
                    String unitsStr = readCol(cols, hm, "units");
                    String priceStr = readCol(cols, hm, "price");

                    LocalDate txnDate = parseDateLenient(tradeDate);
                    BigDecimal units = parseBigDecimalLenient(unitsStr);
                    BigDecimal amount = parseBigDecimalLenient(amountStr);
                    BigDecimal price = parseBigDecimalLenient(priceStr);
                    String txnType = mapCamsTxnType(transactionType);

                    String schemeCode = productCode != null ? productCode : schemeName;
                    // try mapping via external map
                    try {
                        ExternalSchemeMap esm = mapRepo.findByRtaNameAndExternalCode(rtaName, schemeCode).orElse(null);
                        if (esm != null && esm.getSchemeCode() != null) schemeCode = esm.getSchemeCode();
                    } catch (Exception e) { /* ignore mapping failures */ }

                    String sourceRef = readCol(cols, hm, "source_reference", "reference", "trans id");
                    if (sourceRef == null) {
                        StringBuilder sb = new StringBuilder();
                        if (folio != null) sb.append(folio);
                        if (txnDate != null) {
                            if (sb.length() > 0) sb.append("|");
                            sb.append(txnDate.toString());
                        }
                        if (txnType != null) {
                            if (sb.length() > 0) sb.append("|");
                            sb.append(txnType);
                        }
                        if (amount != null) {
                            if (sb.length() > 0) sb.append("|");
                            sb.append(amount.toPlainString());
                        }
                        if (units != null) {
                            if (sb.length() > 0) sb.append("|");
                            sb.append(units.toPlainString());
                        }
                        sourceRef = sb.toString();
                    }

                    UserTransaction txn = UserTransaction.builder()
                            .userId(userId)
                            .txnSource(rtaName)
                            .sourceReference(sourceRef)
                            .txnDate(txnDate)
                            .schemeCode(schemeCode)
                            .txnType(TxnType.valueOf(txnType))
                            .units(units == null ? BigDecimal.ZERO : units)
                            .amount(amount == null ? BigDecimal.ZERO : amount)
                            .pricePerUnit(price)
                            .remarks(mfName + (schemeName != null ? " | " + schemeName : ""))
                            .importId(importId)
                            .createdAt(java.time.LocalDateTime.now())
                            .build();

                    consumer.accept(txn);
                } catch (Exception ex) {
                    log.trace("Error parsing CAMS transaction line {}: {}", lineno, ex.getMessage());
                }
            }
        }
    }

    /**
     * Stream parse valuation TSV and call consumer for each parsed UserHolding.
     */
    public void streamValuation(InputStream is, Long userId, String rtaName, Consumer<UserHolding> consumer) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String header = br.readLine();
            if (header == null) return;
            Map<String, Integer> hm = headerMap(header, "\t");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split("\t", -1);
                try {
                    String schemeCode = readCol(cols, hm, "schemecode", "product_code", "scheme code");
                    String schemeName = readCol(cols, hm, "scheme", "scheme name");
                    String unitBalanceStr = readCol(cols, hm, "unit balance", "unit_balance", "units");
                    String navDateStr = readCol(cols, hm, "nav date", "nav_date");
                    String currentValueStr = readCol(cols, hm, "current value(rs.)", "current value", "current value (rs.)");
                    String costValueStr = readCol(cols, hm, "cost value(rs.)", "cost value");

                    BigDecimal units = parseBigDecimalLenient(unitBalanceStr);
                    LocalDate navDate = parseDateLenient(navDateStr);
                    BigDecimal costValue = parseBigDecimalLenient(costValueStr);

                    if (units == null || units.compareTo(BigDecimal.ZERO) == 0) continue;

                    BigDecimal avgCost = null;
                    if (costValue != null) avgCost = costValue.divide(units, 8, BigDecimal.ROUND_HALF_UP);

                    String canonical = schemeCode != null ? schemeCode : schemeName;
                    try {
                        ExternalSchemeMap esm = mapRepo.findByRtaNameAndExternalCode(rtaName, canonical).orElse(null);
                        if (esm != null && esm.getSchemeCode() != null) canonical = esm.getSchemeCode();
                    } catch (Exception e) {
                        log.debug("Ignored mapping error for scheme code: {}", canonical);
                    }

                    UserHolding h = UserHolding.builder()
                            .userId(userId)
                            .schemeCode(canonical)
                            .units(units)
                            .totalCost(costValue == null ? java.math.BigDecimal.ZERO : costValue)
                            .avgCostPerUnit(avgCost)
                            .lastUpdated(java.time.LocalDateTime.now())
                            .build();

                    consumer.accept(h);
                } catch (Exception ex) {
                    log.warn("Error parsing CAMS valuation line: {}", ex.getMessage());
                }
            }
        }
    }
}

