package com.example.portfoliotracker.service;

import com.example.portfoliotracker.repository.PortfolioUserRepository;
import com.example.portfoliotracker.repository.UserHoldingRepository;
import com.example.portfoliotracker.repository.UserTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ResetService {

    private final PortfolioUserRepository portfolioUserRepository;
    private final UserTransactionRepository userTransactionRepository;
    private final UserHoldingRepository userHoldingRepository;

    public ResetService(PortfolioUserRepository portfolioUserRepository,
                        UserTransactionRepository userTransactionRepository,
                        UserHoldingRepository userHoldingRepository) {
        this.portfolioUserRepository = portfolioUserRepository;
        this.userTransactionRepository = userTransactionRepository;
        this.userHoldingRepository = userHoldingRepository;
    }

    /**
     * Reset all user-related data: clears user transactions, holdings, and users.
     * This is a destructive operation and should only be used for testing/development.
     *
     * @return a summary of what was deleted
     */
    @Transactional
    public ResetSummary resetUserData() {
        log.warn("Resetting all user-related data");

        long transactionCount = userTransactionRepository.count();
        long holdingCount = userHoldingRepository.count();
        long userCount = portfolioUserRepository.count();

        // Delete in order of foreign key dependencies: transactions -> holdings -> users
        userTransactionRepository.deleteAll();
        log.info("Deleted {} user transactions", transactionCount);

        userHoldingRepository.deleteAll();
        log.info("Deleted {} user holdings", holdingCount);

        portfolioUserRepository.deleteAll();
        log.info("Deleted {} users", userCount);

        ResetSummary summary = new ResetSummary();
        summary.setTransactionsDeleted(transactionCount);
        summary.setHoldingsDeleted(holdingCount);
        summary.setUsersDeleted(userCount);
        summary.setStatus("SUCCESS");
        summary.setMessage("All user-related data has been cleared");

        return summary;
    }

    /**
     * Summary DTO for reset operation results.
     */
    @lombok.Getter
    @lombok.Setter
    public static class ResetSummary {
        private long usersDeleted;
        private long transactionsDeleted;
        private long holdingsDeleted;
        private String status;
        private String message;
    }
}

