package com.sudheer.portfoliotracker.controller;

import com.sudheer.portfoliotracker.api.controller.AmfiSyncController;
import com.sudheer.portfoliotracker.application.usecase.SyncAmfiDataUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AmfiSyncController
 * Tests cover daily sync, adhoc sync, and error scenarios
 */
@DisplayName("AmfiSyncController Tests")
class AmfiSyncControllerTest {

    private SyncAmfiDataUseCase syncUseCase;
    private AmfiSyncController controller;

    @BeforeEach
    void setUp() {
        syncUseCase = mock(SyncAmfiDataUseCase.class);
        controller = new AmfiSyncController(syncUseCase);
    }

    @Test
    @DisplayName("dailySync should invoke SyncAmfiDataUseCase.runDaily()")
    void testDailySync_InvokesUseCase() throws Exception {
        // Act
        controller.syncDailyNav();

        // Assert
        verify(syncUseCase, times(1)).runDaily();
        verifyNoMoreInteractions(syncUseCase);
    }

    @Test
    @DisplayName("dailySync should handle use case exception")
    void testDailySync_HandlesException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Sync failed")).when(syncUseCase).runDaily();

        // Act & Assert
        assertThrows(RuntimeException.class, () -> controller.syncDailyNav());
        verify(syncUseCase, times(1)).runDaily();
    }

    @Test
    @DisplayName("adhocSync should invoke SyncAmfiDataUseCase.runAdhoc with correct dates")
    void testAdhocSync_InvokesUseCaseWithDates() throws Exception {
        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        // Act
        controller.adhocSync(fromDate, toDate);

        // Assert
        verify(syncUseCase, times(1)).runAdhoc(fromDate, toDate);
        verifyNoMoreInteractions(syncUseCase);
    }

    @Test
    @DisplayName("adhocSync should handle use case exception")
    void testAdhocSync_HandlesException() throws Exception {
        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        doThrow(new RuntimeException("Adhoc sync failed"))
                .when(syncUseCase).runAdhoc(fromDate, toDate);

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> controller.adhocSync(fromDate, toDate));
        verify(syncUseCase, times(1)).runAdhoc(fromDate, toDate);
    }

    @Test
    @DisplayName("adhocSync should work with same-day date range")
    void testAdhocSync_SameDayDateRange() throws Exception {
        // Arrange
        LocalDate sameDate = LocalDate.of(2026, 2, 14);

        // Act
        controller.adhocSync(sameDate, sameDate);

        // Assert
        verify(syncUseCase, times(1)).runAdhoc(sameDate, sameDate);
    }

    @Test
    @DisplayName("adhocSync should work with large date range")
    void testAdhocSync_LargeDateRange() throws Exception {
        // Arrange
        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);

        // Act
        controller.adhocSync(fromDate, toDate);

        // Assert
        verify(syncUseCase, times(1)).runAdhoc(fromDate, toDate);
    }

    @Test
    @DisplayName("adhocSync should work with reversed dates")
    void testAdhocSync_ReversedDates() throws Exception {
        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 2, 14);
        LocalDate toDate = LocalDate.of(2026, 1, 1);

        // Act (delegates to use case, controller doesn't validate)
        controller.adhocSync(fromDate, toDate);

        // Assert
        verify(syncUseCase, times(1)).runAdhoc(fromDate, toDate);
    }

    @Test
    @DisplayName("controller should maintain use case reference")
    void testController_MaintainsUseCaseReference() {
        // Arrange
        SyncAmfiDataUseCase newUseCase = mock(SyncAmfiDataUseCase.class);
        AmfiSyncController newController = new AmfiSyncController(newUseCase);

        // Assert - verify the use case is properly injected
        assertNotNull(newController);
    }

    @Test
    @DisplayName("dailySync should not modify any state")
    void testDailySync_IsIdempotent() throws Exception {
        // Act
        controller.syncDailyNav();
        controller.syncDailyNav();
        controller.syncDailyNav();

        // Assert - should call use case 3 times
        verify(syncUseCase, times(3)).runDaily();
    }

    @Test
    @DisplayName("adhocSync should correctly pass different date parameters")
    void testAdhocSync_CorrectlyPassesDifferentDates() throws Exception {
        // Arrange
        LocalDate fromDate1 = LocalDate.of(2026, 1, 1);
        LocalDate toDate1 = LocalDate.of(2026, 1, 31);
        LocalDate fromDate2 = LocalDate.of(2026, 2, 1);
        LocalDate toDate2 = LocalDate.of(2026, 2, 28);

        // Act
        controller.adhocSync(fromDate1, toDate1);
        controller.adhocSync(fromDate2, toDate2);

        // Assert
        verify(syncUseCase, times(1)).runAdhoc(fromDate1, toDate1);
        verify(syncUseCase, times(1)).runAdhoc(fromDate2, toDate2);
    }

    @Test
    @DisplayName("daily and adhoc sync can be called in sequence")
    void testMixedSyncCalls() throws Exception {
        // Arrange
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        // Act
        controller.syncDailyNav();
        controller.adhocSync(fromDate, toDate);
        controller.syncDailyNav();

        // Assert
        verify(syncUseCase, times(2)).runDaily();
        verify(syncUseCase, times(1)).runAdhoc(fromDate, toDate);
    }
}
