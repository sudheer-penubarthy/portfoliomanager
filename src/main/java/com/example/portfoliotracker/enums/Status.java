package com.example.portfoliotracker.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Status {
    PENDING,
    PROCESSING,
    COMPLETED,
    IN_PROGRESS,
    FAILED;

    @JsonCreator
    public static Status fromString(String s) {
        if (s == null) return null;
        String cleaned = s.trim();
        // match enum names case\-insensitively
        for (Status st : values()) {
            if (st.name().equalsIgnoreCase(cleaned)) return st;
        }
        // map common synonyms
        switch (cleaned.toLowerCase(Locale.ENGLISH)) {
            case "done":
            case "complete":
                return COMPLETED;
            case "inprogress":
            case "in-progress":
                return IN_PROGRESS;
            default:
                throw new IllegalArgumentException("Unknown Status: " + s);
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
