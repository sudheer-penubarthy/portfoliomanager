package com.sudheer.portfoliotracker.api.controller;

import com.sudheer.portfoliotracker.api.dto.PortfolioSummaryDto;
import com.sudheer.portfoliotracker.api.dto.HoldingDto;
import com.sudheer.portfoliotracker.api.dto.TransactionDetailsDto;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.PortfolioUser;
import com.sudheer.portfoliotracker.repository.PortfolioUserRepository;
import com.sudheer.portfoliotracker.service.PortfolioService;
import com.sudheer.portfoliotracker.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioUserRepository userRepository;

    public PortfolioController(PortfolioService portfolioService,
                              PortfolioUserRepository userRepository) {
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
    }

    /**
     * Get portfolio summary for a user
     *
     * @param userId the user ID
     * @return 200 OK with portfolio summary (total value, P&L, XIRR, etc.)
     */
    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummary(@RequestParam Long userId) {
        log.debug("Fetching portfolio summary for user: {}", userId);

        PortfolioUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary(userId, user.getEmail(), user.getName());
        return ResponseEntity.ok(summary);
    }

    /**
     * Get portfolio summary by email
     *
     * @param email the user email
     * @return 200 OK with portfolio summary
     */
    @GetMapping("/summary/by-email")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummaryByEmail(@RequestParam String email) {
        log.debug("Fetching portfolio summary for email: {}", email);

        PortfolioUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary(user.getId(), user.getEmail(), user.getName());
        return ResponseEntity.ok(summary);
    }

    /**
     * Get transaction details for a specific fund
     *
     * @param userId     the user ID
     * @param schemeCode the scheme code (fund code)
     * @return 200 OK with list of transactions for the fund
     */
    @GetMapping("/fund/{schemeCode}/transactions")
    public ResponseEntity<List<TransactionDetailsDto>> getFundTransactions(
            @RequestParam Long userId,
            @PathVariable String schemeCode) {
        log.debug("Fetching transactions for user: {}, scheme: {}", userId, schemeCode);

        List<TransactionDetailsDto> transactions = portfolioService.getFundTransactions(userId, schemeCode);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get holdings grouped by fund
     *
     * @param userId the user ID
     * @return 200 OK with map of scheme code to units held
     */
    @GetMapping("/holdings")
    public ResponseEntity<Map<String, java.math.BigDecimal>> getHoldingsByFund(@RequestParam Long userId) {
        log.debug("Fetching holdings by fund for user: {}", userId);

        Map<String, java.math.BigDecimal> holdings = portfolioService.getHoldingsByFund(userId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get detailed holdings with invested and current values.
     *
     * @param userId the user ID
     * @return 200 OK with detailed holdings list
     */
    @GetMapping("/holdings/details")
    public ResponseEntity<List<HoldingDto>> getHoldingDetails(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.debug("Fetching holding details for user: {} (includeInactive={})", userId, includeInactive);

        List<HoldingDto> holdings = portfolioService.getHoldingDetails(userId, includeInactive);
        return ResponseEntity.ok(holdings);
    }
}

