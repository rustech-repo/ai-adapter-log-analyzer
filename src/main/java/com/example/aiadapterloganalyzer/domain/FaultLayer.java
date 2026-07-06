package com.example.aiadapterloganalyzer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum FaultLayer {
    SDK("SDK"),
    CORE("Core"),
    API("API"),
    INFRASTRUCTURE("Infrastructure"),
    EXTERNAL("External");

    private final String value;

    FaultLayer(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static FaultLayer fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown fault_layer: " + value));
    }
}
