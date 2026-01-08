package com.example.portfoliotracker.service;

import com.example.portfoliotracker.repository.PortfolioUserRepository;
import com.example.portfoliotracker.repository.UserHoldingRepository;
import com.example.portfoliotracker.repository.UserTransactionRepository;
import com.example.portfoliotracker.service.ResetService.ResetSummary;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResetServiceTest {

    @Test
    void resetUserData_deletesAllUserRelatedData() {
        // Setup mocks
        PortfolioUserRepository userRepo = mock(PortfolioUserRepository.class);
        UserTransactionRepository txnRepo = mock(UserTransactionRepository.class);
        UserHoldingRepository holdingRepo = mock(UserHoldingRepository.class);

        when(userRepo.count()).thenReturn(5L);
        when(txnRepo.count()).thenReturn(10L);
        when(holdingRepo.count()).thenReturn(8L);

        ResetService service = new ResetService(userRepo, txnRepo, holdingRepo);

        // Execute
        ResetSummary summary = service.resetUserData();

        // Verify
        assertEquals(5L, summary.getUsersDeleted());
        assertEquals(10L, summary.getTransactionsDeleted());
        assertEquals(8L, summary.getHoldingsDeleted());
        assertEquals("SUCCESS", summary.getStatus());
        assertEquals("All user-related data has been cleared", summary.getMessage());

        // Verify deleteAll was called in correct order (transactions first, then holdings, then users)
        verify(txnRepo, times(1)).deleteAll();
        verify(holdingRepo, times(1)).deleteAll();
        verify(userRepo, times(1)).deleteAll();

        InOrder inOrder = inOrder(txnRepo, holdingRepo, userRepo);
        inOrder.verify(txnRepo).deleteAll();
        inOrder.verify(holdingRepo).deleteAll();
        inOrder.verify(userRepo).deleteAll();
    }

    @Test
    void resetUserData_handlesEmptyData() {
        // Setup mocks with zero counts
        PortfolioUserRepository userRepo = mock(PortfolioUserRepository.class);
        UserTransactionRepository txnRepo = mock(UserTransactionRepository.class);
        UserHoldingRepository holdingRepo = mock(UserHoldingRepository.class);

        when(userRepo.count()).thenReturn(0L);
        when(txnRepo.count()).thenReturn(0L);
        when(holdingRepo.count()).thenReturn(0L);

        ResetService service = new ResetService(userRepo, txnRepo, holdingRepo);

        // Execute
        ResetSummary summary = service.resetUserData();

        // Verify
        assertEquals(0L, summary.getUsersDeleted());
        assertEquals(0L, summary.getTransactionsDeleted());
        assertEquals(0L, summary.getHoldingsDeleted());
        assertEquals("SUCCESS", summary.getStatus());

        verify(txnRepo, times(1)).deleteAll();
        verify(holdingRepo, times(1)).deleteAll();
        verify(userRepo, times(1)).deleteAll();
    }
}

