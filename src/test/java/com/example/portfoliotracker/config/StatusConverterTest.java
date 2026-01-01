package com.example.portfoliotracker.config;

import com.example.portfoliotracker.enums.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusConverterTest {

    private final StatusConverter converter = new StatusConverter();

    @Test
    void convertsUpperCase() {
        assertEquals(Status.COMPLETED, converter.convert("COMPLETED"));
    }

    @Test
    void convertsLowerCase() {
        assertEquals(Status.COMPLETED, converter.convert("completed"));
    }

    @Test
    void convertsSynonym() {
        assertEquals(Status.COMPLETED, converter.convert("done"));
    }

    @Test
    void nullReturnsNull() {
        assertNull(converter.convert(null));
    }
}

