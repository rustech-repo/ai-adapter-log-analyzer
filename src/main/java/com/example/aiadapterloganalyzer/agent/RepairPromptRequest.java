package com.example.aiadapterloganalyzer.agent;

public record RepairPromptRequest(
        String invalidOutput,
        String validationError,
        LogSignals signals,
        String architectureContext
) {
}
