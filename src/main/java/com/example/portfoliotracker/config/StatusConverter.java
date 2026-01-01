package com.example.portfoliotracker.config;

import com.example.portfoliotracker.enums.Status;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to allow case-insensitive and synonym-aware conversion of request
 * parameter values to the Status enum.
 *
 * Spring Boot will auto-detect Converter beans and register them with the
 * ConversionService used for @RequestParam/@PathVariable binding.
 */
@Component
public class StatusConverter implements Converter<String, Status> {

    @Override
    public Status convert(String source) {
        if (source == null) return null;
        return Status.fromString(source);
    }
}

