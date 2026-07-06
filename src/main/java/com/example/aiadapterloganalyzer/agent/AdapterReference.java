package com.example.aiadapterloganalyzer.agent;

public record AdapterReference(
        String code,
        String provider
) {

    public String displayName() {
        return code + " (" + provider + ")";
    }
}
