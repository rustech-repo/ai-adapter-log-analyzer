package com.example.aiadapterloganalyzer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ActionRisk {
    SAFE("safe"),
    CAUTION("caution"),
    RISKY("risky");

    private final String value;

    ActionRisk(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ActionRisk fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown risk: " + value));
    }
}
