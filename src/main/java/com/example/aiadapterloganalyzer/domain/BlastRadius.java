package com.example.aiadapterloganalyzer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum BlastRadius {
    SINGLE_MERCHANT("single_merchant"),
    SINGLE_ADAPTER("single_adapter"),
    MULTI_ADAPTER("multi_adapter"),
    PLATFORM_WIDE("platform_wide");

    private final String value;

    BlastRadius(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static BlastRadius fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown blast_radius: " + value));
    }
}
