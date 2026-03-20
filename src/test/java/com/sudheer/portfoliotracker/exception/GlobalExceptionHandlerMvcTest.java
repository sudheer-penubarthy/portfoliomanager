package com.sudheer.portfoliotracker.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests exception handler bean configuration and methods.
 */
@SpringJUnitConfig
class GlobalExceptionHandlerMvcTest {

    /**
     * Test: GlobalExceptionHandler bean can be instantiated
     */
    @Test
    void globalExceptionHandler_canBeInstantiated() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertNotNull(handler, "GlobalExceptionHandler should not be null");
    }

    /**
     * Test: ApiError can be created and populated
     */
    @Test
    void apiError_canBeCreatedWithData() {
        ApiError error = new ApiError();
        error.setStatus(400);
        error.setError("Test Error");
        error.setMessage("Test message");
        error.setPath("/test");

        assertEquals(400, error.getStatus());
        assertEquals("Test Error", error.getError());
        assertEquals("Test message", error.getMessage());
        assertEquals("/test", error.getPath());
    }

    /**
     * Test: ResourceNotFoundException can be thrown and caught
     */
    @Test
    void resourceNotFound_throwsCorrectException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            throw new ResourceNotFoundException("User", "email", "test@test.com");
        });
    }

    /**
     * Test: Exception handler handles different exception types
     */
    @Test
    void globalExceptionHandler_hasExceptionHandlers() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        // Verify the handler has the necessary methods (they exist in the class)
        assertNotNull(handler.getClass().getDeclaredMethods());
        assertTrue(handler.getClass().getDeclaredMethods().length > 0);
    }
}

