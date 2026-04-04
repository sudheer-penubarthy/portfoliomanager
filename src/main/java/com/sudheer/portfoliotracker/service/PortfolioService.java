package com.sudheer.portfoliotracker.service;

import com.sudheer.portfoliotracker.api.dto.HoldingDto;
import com.sudheer.portfoliotracker.api.dto.PortfolioSummaryDto;
import com.sudheer.portfoliotracker.api.dto.SchemeGoalAllocationDto;
import com.sudheer.portfoliotracker.api.dto.TransactionDetailsDto;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UserHolding;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UserTransaction;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiNav;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiScheme;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.GoalEntity;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.GoalFundAlignmentEntity;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.PortfolioSnapshot;
import com.sudheer.portfoliotracker.repository.UserHoldingRepository;
import com.sudheer.portfoliotracker.repository.UserTransactionRepository;
import com.sudheer.portfoliotracker.repository.AmfiNavRepository;
import com.sudheer.portfoliotracker.repository.AmfiSchemeRepository;
import com.sudheer.portfoliotracker.repository.GoalFundAlignmentRepository;
import com.sudheer.portfoliotracker.repository.GoalRepository;
import com.sudheer.portfoliotracker.repository.PortfolioSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortfolioService {

    private final UserHoldingRepository holdingRepository;
    private final UserTransactionRepository transactionRepository;
    private final AmfiNavRepository navRepository;
    private final AmfiSchemeRepository schemeRepository;
    private final GoalRepository goalRepository;
    private final GoalFundAlignmentRepository goalFundAlignmentRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final SchemeRegistry schemeRegistry;

    public PortfolioService(UserHoldingRepository holdingRepository,
                           UserTransactionRepository transactionRepository,
                           AmfiNavRepository navRepository,
                           AmfiSchemeRepository schemeRepository,
                           GoalRepository goalRepository,
                           GoalFundAlignmentRepository goalFundAlignmentRepository,
                           PortfolioSnapshotRepository portfolioSnapshotRepository,
                           SchemeRegistry schemeRegistry) {
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.navRepository = navRepository;
        this.schemeRepository = schemeRepository;
        this.goalRepository = goalRepository;
        this.goalFundAlignmentRepository = goalFundAlignmentRepository;
        this.portfolioSnapshotRepository = portfolioSnapshotRepository;
        this.schemeRegistry = schemeRegistry;
    }

    /**
     * Get portfolio summary for a user
     */
    public PortfolioSummaryDto getPortfolioSummary(Long userId, String email, String name) {
        log.debug("Calculating portfolio summary for user: {}", userId);

        List<UserHolding> holdings = holdingRepository.findByUserId(userId);
        Map<String, String> fallbackSchemeNames = buildFallbackSchemeNameMap(userId);
        Map<String, AmfiScheme> resolvedSchemes = resolveSchemesForHoldings(holdings, fallbackSchemeNames);

        BigDecimal totalInvestedAmount = BigDecimal.ZERO;
        BigDecimal currentPortfolioValue = BigDecimal.ZERO;
        BigDecimal previousPortfolioValue = BigDecimal.ZERO;
        LocalDate latestNavDate = null;
        BigDecimal holdingsSnapshotValue = BigDecimal.ZERO;
        LocalDate holdingsSnapshotDate = null;
        BigDecimal equityValue = BigDecimal.ZERO;
        BigDecimal debtValue = BigDecimal.ZERO;

        for (UserHolding holding : holdings) {
            totalInvestedAmount = totalInvestedAmount.add(holding.getTotalCost());

            Optional<AmfiNav> latestNav = findLatestNav(holding, resolvedSchemes.get(holding.getSchemeCode()));
            if (latestNav.isPresent()) {
                BigDecimal currentValue = holding.getUnits().multiply(latestNav.get().getNavValue());
                currentPortfolioValue = currentPortfolioValue.add(currentValue);
                BigDecimal previousValue = findPreviousNavValue(holding, resolvedSchemes.get(holding.getSchemeCode()));
                previousPortfolioValue = previousPortfolioValue.add(previousValue);
                LocalDate navDate = latestNav.get().getNavDate();
                if (navDate != null && (latestNavDate == null || navDate.isAfter(latestNavDate))) {
                    latestNavDate = navDate;
                }
                if (isDebtLike(resolvedSchemes.get(holding.getSchemeCode()))) {
                    debtValue = debtValue.add(currentValue);
                } else {
                    equityValue = equityValue.add(currentValue);
                }
            }

            if (holding.getSnapshotCurrentValue() != null) {
                holdingsSnapshotValue = holdingsSnapshotValue.add(holding.getSnapshotCurrentValue());
            }
            if (holding.getSnapshotNavDate() != null && (holdingsSnapshotDate == null || holding.getSnapshotNavDate().isAfter(holdingsSnapshotDate))) {
                holdingsSnapshotDate = holding.getSnapshotNavDate();
            }
        }

        Optional<PortfolioSnapshot> latestPortfolioSnapshot = portfolioSnapshotRepository.findTopByUserIdOrderBySnapshotDateDescIdDesc(userId);
        LocalDate latestSnapshotDate = holdingsSnapshotDate;
        BigDecimal latestSnapshotValue = holdingsSnapshotDate != null ? holdingsSnapshotValue : null;
        String snapshotDetailLevel = holdingsSnapshotDate != null ? "FUND" : null;

        if (latestPortfolioSnapshot.isPresent()) {
            PortfolioSnapshot portfolioSnapshot = latestPortfolioSnapshot.get();
            if (latestSnapshotDate == null || portfolioSnapshot.getSnapshotDate().isAfter(latestSnapshotDate)) {
                latestSnapshotDate = portfolioSnapshot.getSnapshotDate();
                latestSnapshotValue = portfolioSnapshot.getTotalMarketValue();
                snapshotDetailLevel = portfolioSnapshot.getSnapshotDetailLevel();
            }
        }

        BigDecimal totalGain = currentPortfolioValue.subtract(totalInvestedAmount);
        Double gainPercentage = totalInvestedAmount.compareTo(BigDecimal.ZERO) > 0
                ? (totalGain.doubleValue() / totalInvestedAmount.doubleValue()) * 100
                : 0.0;
        BigDecimal dailyGain = currentPortfolioValue.subtract(previousPortfolioValue);
        Double dailyGainPercentage = previousPortfolioValue.compareTo(BigDecimal.ZERO) > 0
                ? dailyGain.divide(previousPortfolioValue, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        BigDecimal valuationDiscrepancyAmount = latestSnapshotDate != null
                && latestSnapshotValue != null
                ? currentPortfolioValue.subtract(latestSnapshotValue)
                : BigDecimal.ZERO;
        Double valuationDiscrepancyPercentage = latestSnapshotValue != null
                && latestSnapshotValue.compareTo(BigDecimal.ZERO) > 0
                ? valuationDiscrepancyAmount.divide(latestSnapshotValue, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        boolean hasValuationDiscrepancy = latestSnapshotDate != null
                && valuationDiscrepancyAmount.abs().compareTo(BigDecimal.ONE) >= 0;

        // Calculate XIRR (simplified - returns annualized return for now)
        Double xirr = calculateXIRR(userId);

        return PortfolioSummaryDto.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .totalInvestedAmount(totalInvestedAmount)
                .currentPortfolioValue(currentPortfolioValue)
                .totalGain(totalGain)
                .gainPercentage(gainPercentage)
                .dailyGain(dailyGain)
                .dailyGainPercentage(dailyGainPercentage)
                .xirr(xirr)
                .latestNavDate(latestNavDate)
                .latestSnapshotDate(latestSnapshotDate)
                .latestSnapshotValue(latestSnapshotDate != null ? latestSnapshotValue : null)
                .snapshotDetailLevel(snapshotDetailLevel)
                .valuationDiscrepancyAmount(latestSnapshotDate != null ? valuationDiscrepancyAmount : null)
                .valuationDiscrepancyPercentage(latestSnapshotDate != null ? valuationDiscrepancyPercentage : null)
                .hasValuationDiscrepancy(hasValuationDiscrepancy)
                .totalFunds(holdings.size())
                .totalHoldings((int) holdings.stream().filter(h -> h.getUnits().compareTo(BigDecimal.ZERO) > 0).count())
                .equityValue(equityValue)
                .debtValue(debtValue)
                .equityPercentage(currentPortfolioValue.compareTo(BigDecimal.ZERO) > 0
                        ? equityValue.divide(currentPortfolioValue, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                        : 0.0)
                .debtPercentage(currentPortfolioValue.compareTo(BigDecimal.ZERO) > 0
                        ? debtValue.divide(currentPortfolioValue, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                        : 0.0)
                .build();
    }

    /**
     * Get transaction details for a fund
     */
    public List<TransactionDetailsDto> getFundTransactions(Long userId, String schemeCode) {
        log.debug("Fetching transactions for user: {}, scheme: {}", userId, schemeCode);

        List<UserTransaction> transactions = transactionRepository.findByUserIdAndSchemeCodeOrderByTxnDateAscIdAsc(userId, schemeCode);
        Optional<AmfiScheme> scheme = schemeRepository.findBySchemeCode(schemeCode);

        return transactions.stream()
                .filter(txn -> !isAdministrativeZeroTxn(txn))
                .map(txn -> TransactionDetailsDto.builder()
                        .id(txn.getId())
                        .schemeCode(txn.getSchemeCode())
                        .schemeName(scheme.map(AmfiScheme::getSchemeName).orElse("Unknown"))
                        .txnType(txn.getTxnType())
                        .txnDate(txn.getTxnDate())
                        .units(txn.getUnits())
                        .amount(txn.getAmount())
                        .pricePerUnit(txn.getPricePerUnit())
                        .remarks(txn.getRemarks())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isAdministrativeZeroTxn(UserTransaction txn) {
        boolean isOther = txn.getTxnType() != null && "OTHER".equals(txn.getTxnType().name());
        boolean zeroUnits = txn.getUnits() == null || txn.getUnits().compareTo(BigDecimal.ZERO) == 0;
        boolean zeroAmount = txn.getAmount() == null || txn.getAmount().compareTo(BigDecimal.ZERO) == 0;
        return isOther && zeroUnits && zeroAmount;
    }

    /**
     * Calculate current value of portfolio for given schemes
     */
    public BigDecimal calculatePortfolioValueForSchemes(Long userId, List<String> schemeCodes) {
        log.debug("Calculating portfolio value for schemes: {}", schemeCodes);

        return calculatePortfolioValuesForSchemes(userId, schemeCodes).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> calculatePortfolioValuesForSchemes(Long userId, Collection<String> schemeCodes) {
        if (schemeCodes == null || schemeCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> normalizedSchemeCodes = schemeCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList();

        if (normalizedSchemeCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, UserHolding> holdingsBySchemeCode = holdingRepository.findByUserIdAndSchemeCodeIn(userId, normalizedSchemeCodes).stream()
                .collect(Collectors.toMap(
                        UserHolding::getSchemeCode,
                        holding -> holding,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        if (holdingsBySchemeCode.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> latestNavBySchemeCode = navRepository.findLatestNavsBySchemeCodesIn(new ArrayList<>(holdingsBySchemeCode.keySet())).stream()
                .collect(Collectors.toMap(
                        AmfiNav::getSchemeCode,
                        AmfiNav::getNavValue,
                        (left, right) -> left,
                        HashMap::new
                ));

        Map<String, BigDecimal> valuesBySchemeCode = new LinkedHashMap<>();
        for (Map.Entry<String, UserHolding> entry : holdingsBySchemeCode.entrySet()) {
            BigDecimal latestNav = latestNavBySchemeCode.get(entry.getKey());
            if (latestNav == null) {
                continue;
            }

            BigDecimal units = entry.getValue().getUnits() == null ? BigDecimal.ZERO : entry.getValue().getUnits();
            valuesBySchemeCode.put(entry.getKey(), units.multiply(latestNav));
        }

        return valuesBySchemeCode;
    }

    /**
     * Calculate XIRR (eXtended Internal Rate of Return)
     * Simplified implementation - returns annualized return
     */
    private Double calculateXIRR(Long userId) {
        log.debug("Calculating XIRR for user: {}", userId);

        List<UserTransaction> transactions = transactionRepository.findByUserId(userId);

        if (transactions.isEmpty()) {
            return 0.0;
        }

        // Group by scheme
        Map<String, List<UserTransaction>> txnsByScheme = transactions.stream()
                .collect(Collectors.groupingBy(UserTransaction::getSchemeCode));

        Double totalXIRR = 0.0;
        int schemeCount = 0;

        for (Map.Entry<String, List<UserTransaction>> entry : txnsByScheme.entrySet()) {
            String schemeCode = entry.getKey();
            List<UserTransaction> schemeTxns = entry.getValue();

            // Simple XIRR calculation (not IRR, but approximation)
            BigDecimal totalInvested = schemeTxns.stream()
                    .map(UserTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Optional<UserHolding> holding = holdingRepository.findByUserIdAndSchemeCode(userId, schemeCode);
            if (holding.isPresent() && totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                Optional<AmfiNav> latestNav = navRepository.findTopBySchemeCodeOrderByNavDateDesc(schemeCode);

                if (latestNav.isPresent()) {
                    BigDecimal currentValue = holding.get().getUnits()
                            .multiply(latestNav.get().getNavValue());
                    BigDecimal gain = currentValue.subtract(totalInvested);
                    Double xirr = (gain.doubleValue() / totalInvested.doubleValue()) * 100;

                    totalXIRR += xirr;
                    schemeCount++;
                }
            }
        }

        return schemeCount > 0 ? totalXIRR / schemeCount : 0.0;
    }

    /**
     * Get holdings grouped by fund
     */
    public Map<String, BigDecimal> getHoldingsByFund(Long userId) {
        log.debug("Fetching holdings by fund for user: {}", userId);

        List<UserHolding> holdings = holdingRepository.findByUserId(userId);

        return holdings.stream()
                .collect(Collectors.toMap(
                        UserHolding::getSchemeCode,
                        UserHolding::getUnits
                ));
    }

    /**
     * Get detailed holdings for a user, including invested/current values.
     * OPTIMIZED: Uses batch NAV queries and cached scheme resolution
     */
    public List<HoldingDto> getHoldingDetails(Long userId, boolean includeInactive) {
        log.debug("Fetching detailed holdings for user: {} (includeInactive={})", userId, includeInactive);

        List<UserHolding> holdings = buildPortfolioHoldings(userId, includeInactive);
        Map<String, String> fallbackSchemeNames = buildFallbackSchemeNameMap(userId);
        Map<String, List<String>> goalNamesBySchemeCode = buildGoalNamesBySchemeCode(userId);
        Map<String, List<SchemeGoalAllocationDto>> goalAllocationsBySchemeCode = buildGoalAllocationsBySchemeCode(userId);
        Map<String, AmfiScheme> resolvedSchemes = resolveSchemesForHoldings(holdings, fallbackSchemeNames);

        // OPTIMIZATION: Batch fetch all NAVs at once instead of per-holding queries
        List<String> schemeCodesToFetch = holdings.stream()
                .map(h -> resolvedSchemes.getOrDefault(h.getSchemeCode(), 
                        new AmfiScheme()).getSchemeCode())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (schemeCodesToFetch.isEmpty()) {
            schemeCodesToFetch = holdings.stream()
                    .map(UserHolding::getSchemeCode)
                    .distinct()
                    .collect(Collectors.toList());
        }
        
        Map<String, AmfiNav> latestNavsBySchemeCode = findLatestNavsBatch(schemeCodesToFetch);
        BigDecimal totalPortfolioValue = holdings.stream()
                .map(holding -> {
                    AmfiScheme resolvedScheme = resolvedSchemes.get(holding.getSchemeCode());
                    String navSchemeCode = resolvedScheme != null ? resolvedScheme.getSchemeCode() : holding.getSchemeCode();
                    AmfiNav nav = latestNavsBySchemeCode.get(navSchemeCode);
                    return nav == null ? BigDecimal.ZERO : holding.getUnits().multiply(nav.getNavValue());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return holdings.stream()
                .map(holding -> {
                    AmfiScheme resolvedScheme = resolvedSchemes.get(holding.getSchemeCode());
                    Optional<AmfiScheme> scheme = Optional.ofNullable(resolvedScheme);
                    
                    // Use pre-fetched NAV from batch query instead of individual lookups
                    String navSchemeCode = resolvedScheme != null ? resolvedScheme.getSchemeCode() : holding.getSchemeCode();
                    AmfiNav nav = latestNavsBySchemeCode.get(navSchemeCode);
                    Optional<AmfiNav> latestNav = Optional.ofNullable(nav);

                    BigDecimal navValue = latestNav.map(AmfiNav::getNavValue).orElse(BigDecimal.ZERO);
                    LocalDate navDate = latestNav.map(AmfiNav::getNavDate).orElse(null);
                    BigDecimal totalCost = holding.getTotalCost() != null ? holding.getTotalCost() : BigDecimal.ZERO;
                    BigDecimal currentValue = holding.getUnits().multiply(navValue);
                    BigDecimal snapshotCurrentValue = holding.getSnapshotCurrentValue();
                    BigDecimal valuationDiscrepancyAmount = snapshotCurrentValue != null
                            ? currentValue.subtract(snapshotCurrentValue)
                            : null;
                    Double valuationDiscrepancyPercentage = snapshotCurrentValue != null
                            && snapshotCurrentValue.compareTo(BigDecimal.ZERO) > 0
                            ? valuationDiscrepancyAmount.divide(snapshotCurrentValue, 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                            : null;
                    BigDecimal pnl = currentValue.subtract(totalCost);
                    Double currentReturnPercentage = totalCost.compareTo(BigDecimal.ZERO) > 0
                            ? pnl.divide(totalCost, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    BigDecimal dailyReturn = currentValue.subtract(findPreviousNavValue(holding, resolvedScheme));
                    BigDecimal previousValue = currentValue.subtract(dailyReturn);
                    Double dailyReturnPercentage = previousValue.compareTo(BigDecimal.ZERO) > 0
                            ? dailyReturn.divide(previousValue, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    Double portfolioWeightPercentage = totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0
                            ? currentValue.divide(totalPortfolioValue, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    Double xirr = totalCost.compareTo(BigDecimal.ZERO) > 0
                            ? pnl.divide(totalCost, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;

                    return createHoldingDto(
                            holding,
                            scheme,
                            fallbackSchemeNames.get(holding.getSchemeCode()),
                            navValue,
                            navDate,
                            totalCost,
                            currentValue,
                            snapshotCurrentValue,
                            holding.getSnapshotNavDate(),
                            valuationDiscrepancyAmount,
                            valuationDiscrepancyPercentage,
                            holding.getUnits() != null && holding.getUnits().compareTo(BigDecimal.ZERO) > 0,
                            pnl,
                            currentReturnPercentage,
                            dailyReturn,
                            dailyReturnPercentage,
                            portfolioWeightPercentage,
                            xirr,
                            goalNamesBySchemeCode.getOrDefault(holding.getSchemeCode(), Collections.emptyList()),
                            goalAllocationsBySchemeCode.getOrDefault(holding.getSchemeCode(), Collections.emptyList())
                    );
                })
                .sorted(Comparator.comparing(HoldingDto::getCurrentValue).reversed())
                .collect(Collectors.toList());
    }

    private HoldingDto createHoldingDto(UserHolding holding,
                                        Optional<AmfiScheme> scheme,
                                        String fallbackSchemeName,
                                        BigDecimal navValue,
                                        LocalDate navDate,
                                        BigDecimal totalCost,
                                        BigDecimal currentValue,
                                        BigDecimal snapshotCurrentValue,
                                        LocalDate snapshotNavDate,
                                        BigDecimal valuationDiscrepancyAmount,
                                        Double valuationDiscrepancyPercentage,
                                        boolean active,
                                        BigDecimal pnl,
                                        Double currentReturnPercentage,
                                        BigDecimal dailyReturn,
                                        Double dailyReturnPercentage,
                                        Double portfolioWeightPercentage,
                                        Double xirr,
                                        List<String> goalNames,
                                        List<SchemeGoalAllocationDto> goalAllocations) {
        return new HoldingDtoBuilder()
                .withSchemeCode(holding.getSchemeCode())
                .withSchemeName(scheme.map(AmfiScheme::getSchemeName).orElse(
                        fallbackSchemeName != null ? fallbackSchemeName : holding.getSchemeCode()))
                .withSchemeType(scheme.map(AmfiScheme::getInstrumentType).orElse("Unknown"))
                .withUnits(holding.getUnits())
                .withLatestNav(navValue)
                .withLatestNavDate(navDate)
                .withAvgCost(holding.getAvgCostPerUnit())
                .withTotalCost(totalCost)
                .withCurrentValue(currentValue)
                .withSnapshotCurrentValue(snapshotCurrentValue)
                .withSnapshotNavDate(snapshotNavDate)
                .withValuationDiscrepancyAmount(valuationDiscrepancyAmount)
                .withValuationDiscrepancyPercentage(valuationDiscrepancyPercentage)
                .withHasValuationDiscrepancy(valuationDiscrepancyAmount != null && valuationDiscrepancyAmount.abs().compareTo(BigDecimal.ONE) >= 0)
                .withActive(active)
                .withPnl(pnl)
                .withCurrentReturnPercentage(currentReturnPercentage)
                .withDailyReturn(dailyReturn)
                .withDailyReturnPercentage(dailyReturnPercentage)
                .withPortfolioWeightPercentage(portfolioWeightPercentage)
                .withXirr(xirr)
                .withGoalNames(goalNames)
                .withGoalAllocations(goalAllocations)
                .build();
    }

    private List<UserHolding> buildPortfolioHoldings(Long userId, boolean includeInactive) {
        List<UserHolding> activeHoldings = new ArrayList<>(holdingRepository.findByUserId(userId));
        if (!includeInactive) {
            return activeHoldings;
        }

        Map<String, UserHolding> holdingsBySchemeCode = activeHoldings.stream()
                .collect(Collectors.toMap(UserHolding::getSchemeCode, holding -> holding, (left, right) -> left, LinkedHashMap::new));

        for (UserTransaction transaction : transactionRepository.findByUserId(userId)) {
            String schemeCode = transaction.getSchemeCode();
            if (schemeCode == null || schemeCode.isBlank() || holdingsBySchemeCode.containsKey(schemeCode)) {
                continue;
            }

            holdingsBySchemeCode.put(schemeCode, UserHolding.builder()
                    .userId(userId)
                    .schemeCode(schemeCode)
                    .units(BigDecimal.ZERO)
                    .avgCostPerUnit(BigDecimal.ZERO)
                    .totalCost(BigDecimal.ZERO)
                    .lastUpdated(transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDate.now().atStartOfDay())
                    .build());
        }

        return new ArrayList<>(holdingsBySchemeCode.values());
    }

    private Map<String, String> buildFallbackSchemeNameMap(Long userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(txn -> txn.getRemarks() != null && !txn.getRemarks().isBlank())
                .collect(Collectors.toMap(
                        UserTransaction::getSchemeCode,
                        txn -> extractSchemeNameFromRemarks(txn.getRemarks(), txn.getSchemeCode()),
                        (existing, replacement) -> existing
                ));
    }

    private String extractSchemeNameFromRemarks(String remarks, String schemeCode) {
        if (remarks == null || remarks.isBlank()) {
            return schemeCode;
        }

        int separatorIndex = remarks.indexOf('|');
        if (separatorIndex >= 0 && separatorIndex < remarks.length() - 1) {
            return remarks.substring(separatorIndex + 1).trim();
        }

        return remarks.trim();
    }

    private Optional<AmfiNav> findLatestNav(UserHolding holding, AmfiScheme resolvedScheme) {
        String navSchemeCode = resolvedScheme != null ? resolvedScheme.getSchemeCode() : holding.getSchemeCode();
        Optional<AmfiNav> latestNav = navRepository.findBySchemeCodeAndNavDate(navSchemeCode, LocalDate.now());
        if (latestNav.isEmpty()) {
            latestNav = navRepository.findTopBySchemeCodeOrderByNavDateDesc(navSchemeCode);
        }
        return latestNav;
    }

    private BigDecimal findPreviousNavValue(UserHolding holding, AmfiScheme resolvedScheme) {
        String navSchemeCode = resolvedScheme != null ? resolvedScheme.getSchemeCode() : holding.getSchemeCode();
        List<AmfiNav> navs = navRepository.findTop2BySchemeCodeOrderByNavDateDesc(navSchemeCode);
        if (navs.size() < 2) {
            return holding.getUnits().multiply(navs.isEmpty() ? BigDecimal.ZERO : navs.get(0).getNavValue());
        }
        return holding.getUnits().multiply(navs.get(1).getNavValue());
    }

    private boolean isDebtLike(AmfiScheme scheme) {
        String instrumentType = scheme != null && scheme.getInstrumentType() != null
                ? scheme.getInstrumentType().toLowerCase(Locale.ROOT)
                : "";
        return instrumentType.contains("bond")
                || instrumentType.contains("debt")
                || instrumentType.contains("cash")
                || instrumentType.contains("arbitrage")
                || instrumentType.contains("liquid")
                || instrumentType.contains("income");
    }

    /**
     * Batch fetch latest NAVs for multiple scheme codes (OPTIMIZED)
     * This reduces N individual queries to 1 batch query
     */
    private Map<String, AmfiNav> findLatestNavsBatch(List<String> schemeCodes) {
        if (schemeCodes == null || schemeCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<AmfiNav> navs = navRepository.findLatestNavsBySchemeCodesIn(schemeCodes);
        return navs.stream()
                .collect(Collectors.toMap(AmfiNav::getSchemeCode, nav -> nav, (left, right) -> left));
    }

    private Map<String, AmfiScheme> resolveSchemesForHoldings(List<UserHolding> holdings, Map<String, String> fallbackSchemeNames) {
        // Use cached schemes instead of loading from DB
        Map<String, AmfiScheme> byCode = schemeRegistry.getAllSchemesByCode();
        Map<String, SchemeRegistry.PreProcessedScheme> preProcessedSchemes = schemeRegistry.getPreProcessedSchemes();

        Map<String, AmfiScheme> resolved = new HashMap<>();
        for (UserHolding holding : holdings) {
            AmfiScheme directMatch = byCode.get(holding.getSchemeCode());
            if (directMatch != null) {
                resolved.put(holding.getSchemeCode(), directMatch);
                continue;
            }

            String fallbackSchemeName = fallbackSchemeNames.get(holding.getSchemeCode());
            if (fallbackSchemeName == null || fallbackSchemeName.isBlank()) {
                continue;
            }

            // Use pre-processed schemes for fuzzy matching
            AmfiScheme fuzzyMatch = findBestSchemeMatch(fallbackSchemeName, preProcessedSchemes);
            if (fuzzyMatch != null) {
                log.info("Resolved holding scheme code {} to AMFI scheme code {} using fallback name {}", holding.getSchemeCode(), fuzzyMatch.getSchemeCode(), fallbackSchemeName);
                resolved.put(holding.getSchemeCode(), fuzzyMatch);
            }
        }

        return resolved;
    }

    private AmfiScheme findBestSchemeMatch(String fallbackSchemeName, Map<String, SchemeRegistry.PreProcessedScheme> preProcessedSchemes) {
        String normalizedTarget = normalizeSchemeName(fallbackSchemeName);
        Set<String> targetTokens = tokenizeSchemeName(normalizedTarget);
        if (targetTokens.isEmpty()) {
            return null;
        }

        AmfiScheme bestMatch = null;
        int bestScore = 0;
        
        // Iterate through pre-processed schemes (normalized and tokenized already)
        for (SchemeRegistry.PreProcessedScheme preProcessed : preProcessedSchemes.values()) {
            Set<String> candidateTokens = preProcessed.tokens();
            if (candidateTokens.isEmpty()) {
                continue;
            }

            int overlap = 0;
            for (String token : targetTokens) {
                if (candidateTokens.contains(token)) {
                    overlap++;
                }
            }

            if (overlap == 0) {
                continue;
            }

            int score = overlap * 10;
            if (preProcessed.normalizedName().contains(normalizedTarget) || normalizedTarget.contains(preProcessed.normalizedName())) {
                score += 15;
            }
            score -= Math.abs(candidateTokens.size() - targetTokens.size());

            if (score > bestScore) {
                bestScore = score;
                bestMatch = preProcessed.scheme();
            }
        }

        return bestScore >= 25 ? bestMatch : null;
    }

    private String normalizeSchemeName(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokenizeSchemeName(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> tokens = new HashSet<>();
        for (String token : normalizedName.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            if (token.length() <= 2) {
                continue;
            }
            if (token.equals("fund") || token.equals("plan") || token.equals("option")) {
                continue;
            }
            if (token.length() <= 5 && token.chars().allMatch(Character::isLetter)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private Map<String, List<String>> buildGoalNamesBySchemeCode(Long userId) {
        try {
            return buildGoalAllocationsBySchemeCode(userId).entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(SchemeGoalAllocationDto::getGoalName)
                                    .filter(Objects::nonNull)
                                    .toList()
                    ));
        } catch (DataAccessException ex) {
            log.warn("Goal tables are not available yet; portfolio goal labels will be skipped: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, List<SchemeGoalAllocationDto>> buildGoalAllocationsBySchemeCode(Long userId) {
        try {
            Map<Long, GoalEntity> goalsById = goalRepository.findByUserIdOrderByTargetDateAsc(userId).stream()
                    .collect(Collectors.toMap(GoalEntity::getId, goal -> goal, (left, right) -> left, LinkedHashMap::new));
            if (goalsById.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, List<SchemeGoalAllocationDto>> allocationsBySchemeCode = new HashMap<>();
            for (GoalFundAlignmentEntity alignment : goalFundAlignmentRepository.findByGoalIdIn(List.copyOf(goalsById.keySet()))) {
                GoalEntity goal = goalsById.get(alignment.getGoalId());
                if (goal == null) {
                    continue;
                }
                allocationsBySchemeCode.computeIfAbsent(alignment.getSchemeCode(), ignored -> new ArrayList<>())
                        .add(SchemeGoalAllocationDto.builder()
                                .goalId(goal.getId())
                                .goalName(goal.getName())
                                .allocationPercentage(alignment.getAllocationPercentage())
                                .build());
            }

            allocationsBySchemeCode.values().forEach(list ->
                    list.sort(Comparator.comparing(dto -> dto.getGoalName() == null ? "" : dto.getGoalName(), String.CASE_INSENSITIVE_ORDER)));
            return allocationsBySchemeCode;
        } catch (DataAccessException ex) {
            log.warn("Goal alignment tables are not available yet; portfolio goal allocations will be skipped: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private static final class HoldingDtoBuilder {
        private final HoldingDto dto = new HoldingDto();

        private HoldingDtoBuilder withSchemeCode(String schemeCode) {
            dto.setSchemeCode(schemeCode);
            return this;
        }

        private HoldingDtoBuilder withSchemeName(String schemeName) {
            dto.setSchemeName(schemeName);
            return this;
        }

        private HoldingDtoBuilder withSchemeType(String schemeType) {
            dto.setSchemeType(schemeType);
            return this;
        }

        private HoldingDtoBuilder withUnits(BigDecimal units) {
            dto.setUnits(units);
            return this;
        }

        private HoldingDtoBuilder withLatestNav(BigDecimal latestNav) {
            dto.setLatestNav(latestNav);
            return this;
        }

        private HoldingDtoBuilder withLatestNavDate(LocalDate latestNavDate) {
            dto.setLatestNavDate(latestNavDate);
            return this;
        }

        private HoldingDtoBuilder withAvgCost(BigDecimal avgCost) {
            dto.setAvgCost(avgCost);
            return this;
        }

        private HoldingDtoBuilder withTotalCost(BigDecimal totalCost) {
            dto.setTotalCost(totalCost);
            return this;
        }

        private HoldingDtoBuilder withCurrentValue(BigDecimal currentValue) {
            dto.setCurrentValue(currentValue);
            return this;
        }

        private HoldingDtoBuilder withSnapshotCurrentValue(BigDecimal snapshotCurrentValue) {
            dto.setSnapshotCurrentValue(snapshotCurrentValue);
            return this;
        }

        private HoldingDtoBuilder withSnapshotNavDate(LocalDate snapshotNavDate) {
            dto.setSnapshotNavDate(snapshotNavDate);
            return this;
        }

        private HoldingDtoBuilder withValuationDiscrepancyAmount(BigDecimal valuationDiscrepancyAmount) {
            dto.setValuationDiscrepancyAmount(valuationDiscrepancyAmount);
            return this;
        }

        private HoldingDtoBuilder withValuationDiscrepancyPercentage(Double valuationDiscrepancyPercentage) {
            dto.setValuationDiscrepancyPercentage(valuationDiscrepancyPercentage);
            return this;
        }

        private HoldingDtoBuilder withHasValuationDiscrepancy(Boolean hasValuationDiscrepancy) {
            dto.setHasValuationDiscrepancy(hasValuationDiscrepancy);
            return this;
        }

        private HoldingDtoBuilder withActive(Boolean active) {
            dto.setActive(active);
            return this;
        }

        private HoldingDtoBuilder withPnl(BigDecimal pnl) {
            dto.setPnl(pnl);
            return this;
        }

        private HoldingDtoBuilder withCurrentReturnPercentage(Double currentReturnPercentage) {
            dto.setCurrentReturnPercentage(currentReturnPercentage);
            return this;
        }

        private HoldingDtoBuilder withDailyReturn(BigDecimal dailyReturn) {
            dto.setDailyReturn(dailyReturn);
            return this;
        }

        private HoldingDtoBuilder withDailyReturnPercentage(Double dailyReturnPercentage) {
            dto.setDailyReturnPercentage(dailyReturnPercentage);
            return this;
        }

        private HoldingDtoBuilder withPortfolioWeightPercentage(Double portfolioWeightPercentage) {
            dto.setPortfolioWeightPercentage(portfolioWeightPercentage);
            return this;
        }

        private HoldingDtoBuilder withXirr(Double xirr) {
            dto.setXirr(xirr);
            return this;
        }

        private HoldingDtoBuilder withGoalNames(List<String> goalNames) {
            dto.setGoalNames(goalNames);
            return this;
        }

        private HoldingDtoBuilder withGoalAllocations(List<SchemeGoalAllocationDto> goalAllocations) {
            dto.setGoalAllocations(goalAllocations);
            return this;
        }

        private HoldingDto build() {
            return dto;
        }
    }
}
