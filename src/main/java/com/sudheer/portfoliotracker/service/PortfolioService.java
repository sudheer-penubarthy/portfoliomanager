package com.sudheer.portfoliotracker.service;

import com.sudheer.portfoliotracker.api.dto.PortfolioSummaryDto;
import com.sudheer.portfoliotracker.api.dto.TransactionDetailsDto;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UserHolding;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UserTransaction;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiNav;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.AmfiScheme;
import com.sudheer.portfoliotracker.repository.UserHoldingRepository;
import com.sudheer.portfoliotracker.repository.UserTransactionRepository;
import com.sudheer.portfoliotracker.repository.AmfiNavRepository;
import com.sudheer.portfoliotracker.repository.AmfiSchemeRepository;
import lombok.extern.slf4j.Slf4j;
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

    public PortfolioService(UserHoldingRepository holdingRepository,
                           UserTransactionRepository transactionRepository,
                           AmfiNavRepository navRepository,
                           AmfiSchemeRepository schemeRepository) {
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.navRepository = navRepository;
        this.schemeRepository = schemeRepository;
    }

    /**
     * Get portfolio summary for a user
     */
    public PortfolioSummaryDto getPortfolioSummary(Long userId, String email, String name) {
        log.debug("Calculating portfolio summary for user: {}", userId);

        List<UserHolding> holdings = holdingRepository.findByUserId(userId);

        BigDecimal totalInvestedAmount = BigDecimal.ZERO;
        BigDecimal currentPortfolioValue = BigDecimal.ZERO;

        for (UserHolding holding : holdings) {
            totalInvestedAmount = totalInvestedAmount.add(holding.getTotalCost());

            // Get latest NAV for this scheme
            Optional<AmfiNav> latestNav = navRepository.findBySchemeCodeAndNavDate(
                    holding.getSchemeCode(),
                    LocalDate.now()
            );

            // If today's NAV not found, try to find the most recent one
            if (latestNav.isEmpty()) {
                latestNav = navRepository.findTopBySchemeCodeOrderByNavDateDesc(holding.getSchemeCode());
            }

            if (latestNav.isPresent()) {
                BigDecimal currentValue = holding.getUnits().multiply(latestNav.get().getNavValue());
                currentPortfolioValue = currentPortfolioValue.add(currentValue);
            }
        }

        BigDecimal totalGain = currentPortfolioValue.subtract(totalInvestedAmount);
        Double gainPercentage = totalInvestedAmount.compareTo(BigDecimal.ZERO) > 0
                ? (totalGain.doubleValue() / totalInvestedAmount.doubleValue()) * 100
                : 0.0;

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
                .xirr(xirr)
                .totalFunds(holdings.size())
                .totalHoldings((int) holdings.stream().filter(h -> h.getUnits().compareTo(BigDecimal.ZERO) > 0).count())
                .build();
    }

    /**
     * Get transaction details for a fund
     */
    public List<TransactionDetailsDto> getFundTransactions(Long userId, String schemeCode) {
        log.debug("Fetching transactions for user: {}, scheme: {}", userId, schemeCode);

        List<UserTransaction> transactions = transactionRepository.findByUserIdAndSchemeCode(userId, schemeCode);
        Optional<AmfiScheme> scheme = schemeRepository.findBySchemeCode(schemeCode);

        return transactions.stream()
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

    /**
     * Calculate current value of portfolio for given schemes
     */
    public BigDecimal calculatePortfolioValueForSchemes(Long userId, List<String> schemeCodes) {
        log.debug("Calculating portfolio value for schemes: {}", schemeCodes);

        BigDecimal totalValue = BigDecimal.ZERO;

        for (String schemeCode : schemeCodes) {
            Optional<UserHolding> holding = holdingRepository.findByUserIdAndSchemeCode(userId, schemeCode);

            if (holding.isPresent()) {
                Optional<AmfiNav> latestNav = navRepository.findBySchemeCodeAndNavDate(schemeCode, LocalDate.now());

                if (latestNav.isEmpty()) {
                    latestNav = navRepository.findTopBySchemeCodeOrderByNavDateDesc(schemeCode);
                }

                if (latestNav.isPresent()) {
                    BigDecimal currentValue = holding.get().getUnits()
                            .multiply(latestNav.get().getNavValue());
                    totalValue = totalValue.add(currentValue);
                }
            }
        }

        return totalValue;
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
}




