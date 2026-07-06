package com.example.aiadapterloganalyzer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum Probability {
    LIKELY("likely"),
    POSSIBLE("possible"),
    UNLIKELY("unlikely");

    private final String value;

    Probability(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Probability fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown probability: " + value));
    }
}
