package com.example.portfoliotracker.enums;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public enum DateFormat {
    DD_MM_YYYY("dd-MM-yyyy"),
    DD_SLASH_MM_SLASH_YYYY("dd/MM/yyyy"),
    DD_MMM_YYYY("dd-MMM-yyyy"),
    YYYY_MM_DD("yyyy-MM-dd");

    private final DateTimeFormatter formatter;

    DateFormat(String pattern) {
        this.formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH);
    }

    public DateTimeFormatter getFormatter() {
        return formatter;
    }
}
