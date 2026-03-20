package com.sudheer.portfoliotracker.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum UploadStatus {
    UPLOADED,
    PROCESSING,
    COMPLETED,
    FAILED;

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static UploadStatus fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UploadStatus.valueOf(value.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.name();
    }
}

